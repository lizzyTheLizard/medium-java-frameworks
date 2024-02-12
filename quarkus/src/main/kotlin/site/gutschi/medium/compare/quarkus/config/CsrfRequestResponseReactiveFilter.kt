package site.gutschi.medium.compare.quarkus.config

import io.quarkus.csrf.reactive.runtime.CsrfReactiveConfig
import io.quarkus.csrf.reactive.runtime.CsrfTokenUtils
import io.vertx.core.http.impl.CookieImpl
import io.vertx.core.http.impl.ServerCookie
import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.core.Response
import org.jboss.logging.Logger
import org.jboss.resteasy.reactive.server.ServerRequestFilter
import org.jboss.resteasy.reactive.server.ServerResponseFilter
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext
import java.security.SecureRandom
import java.util.*


/** This is a copy of io.quarkus.csrf.reactive.runtime.CsrfRequestResponseReactiveFilter,
 * but it does not read the body as this is not required here and does not work with reactive JSON requests
 * (see https://github.com/quarkusio/quarkus/issues/38326)
 * TODO: After updating to 3.7 (blocked by https://github.com/quarkusio/quarkus/issues/38533) remove this and enable csrf in the config
 **/
@ApplicationScoped
class CsrfRequestResponseReactiveFilter(@Suppress("CdiInjectionPointsInspection") private val config: CsrfReactiveConfig) {
    private val secureRandom = SecureRandom()
    private val logger = Logger.getLogger(CsrfRequestResponseReactiveFilter::class.java)
    private val csrfTokenKey = "csrf_token"
    private val csrfTokenBytesKey = "csrf_token_bytes"
    private val csrfTokenVerifiedKey = "csrf_token_verified"

    @ServerRequestFilter
    fun filter(requestContext: ResteasyReactiveContainerRequestContext, routing: RoutingContext) {
        val cookieToken = getCookieToken(routing, config)
        if (cookieToken != null) {
            try {
                val cookieTokenSize = Base64.getUrlDecoder().decode(cookieToken).size
                // HMAC SHA256 output is 32 bytes long
                val expectedCookieTokenSize = if (config.tokenSignatureKey.isPresent) 32 else config.tokenSize
                if (cookieTokenSize != expectedCookieTokenSize) {
                    logger.debugf(
                        "Invalid CSRF token cookie size: expected %d, got %d", expectedCookieTokenSize,
                        cookieTokenSize
                    )
                    requestContext.abortWith(badClientRequest())
                    return
                }
            } catch (e: IllegalArgumentException) {
                logger.debugf("Invalid CSRF token cookie: %s", cookieToken)
                requestContext.abortWith(badClientRequest())
                return
            }
        }
        if (requestMethodIsSafe(requestContext)) {
            // safe HTTP method, tolerate the absence of a token
            if (cookieToken == null && isCsrfTokenRequired(routing, config)) {
                // Set the CSRF cookie with a randomly generated value
                val tokenBytes = ByteArray(config.tokenSize)
                secureRandom.nextBytes(tokenBytes)
                routing.put(csrfTokenBytesKey, tokenBytes)
                routing.put(csrfTokenKey, Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes))
            }
        } else if (config.verifyToken) {
            // unsafe HTTP method, token is required

            // Check the header first
            val csrfTokenHeaderParam = requestContext.getHeaderString(config.tokenHeaderName)
            if (csrfTokenHeaderParam != null) {
                logger.debugf("CSRF token found in the token header")
                verifyCsrfToken(requestContext, routing, config, cookieToken, csrfTokenHeaderParam)
                return
            } else {
                logger.debugf("No CSRF token found in the token header")
                requestContext.abortWith(badClientRequest())
                return
            }
        } else if (cookieToken == null) {
            logger.debug("CSRF token is not found")
            requestContext.abortWith(badClientRequest())
        }
    }

    private fun verifyCsrfToken(
        requestContext: ResteasyReactiveContainerRequestContext, routing: RoutingContext,
        config: CsrfReactiveConfig, cookieToken: String?, csrfToken: String?
    ) {
        if (cookieToken == null) {
            logger.debug("CSRF cookie is not found")
            requestContext.abortWith(badClientRequest())
            return
        }
        if (csrfToken == null) {
            logger.debug("CSRF token is not found")
            requestContext.abortWith(badClientRequest())
            return
        } else {
            val expectedCookieTokenValue: String = if (config.tokenSignatureKey.isPresent) CsrfTokenUtils.signCsrfToken(
                csrfToken,
                config.tokenSignatureKey.get()
            ) else csrfToken
            if (cookieToken != expectedCookieTokenValue) {
                logger.debug("CSRF token value is wrong")
                requestContext.abortWith(badClientRequest())
                return
            } else {
                routing.put(csrfTokenKey, csrfToken)
                routing.put(csrfTokenVerifiedKey, true)
                return
            }
        }
    }

    /**
     * If the requirements below are true, sets a cookie by the name {@value #CSRF_TOKEN_KEY} that contains a CSRF token.
     *
     *  * The request method is `GET`.
     *  * The request does not contain a valid CSRF token cookie.
     *
     *
     * @throws IllegalStateException if the [RoutingContext] does not have a value for the key {@value #CSRF_TOKEN_KEY}
     * and a cookie needs to be set.
     */
    @ServerResponseFilter
    fun filter(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext?, routing: RoutingContext
    ) {
        if (requestContext.method == "GET" && isCsrfTokenRequired(routing, config) && getCookieToken(
                routing,
                config
            ) == null
        ) {
            val cookieValue: String?
            if (config.tokenSignatureKey.isPresent) {
                val csrfTokenBytes = routing.get<Any>(csrfTokenBytesKey) as ByteArray?
                if (csrfTokenBytes == null) {
                    logger.debug(
                        "CSRF Request Filter did not set the property " + csrfTokenBytesKey
                                + ", no CSRF cookie will be created"
                    )
                    return
                }
                cookieValue = CsrfTokenUtils.signCsrfToken(csrfTokenBytes, config.tokenSignatureKey.get())
            } else {
                val csrfToken = routing.get<Any>(csrfTokenKey) as String?
                if (csrfToken == null) {
                    logger.debug(
                        "CSRF Request Filter did not set the property " + csrfTokenKey
                                + ", no CSRF cookie will be created"
                    )
                    return
                }
                cookieValue = csrfToken
            }
            createCookie(cookieValue, routing, config)
        }
    }

    /**
     * Gets the CSRF token from the CSRF cookie from the current `RoutingContext`.
     *
     * @return An Optional containing the token, or an empty Optional if the token cookie is not present or is invalid
     */
    private fun getCookieToken(routing: RoutingContext, config: CsrfReactiveConfig): String? {
        val cookie = routing.request().getCookie(config.cookieName)
        if (cookie == null) {
            logger.debug("CSRF token cookie is not set")
            return null
        }
        return cookie.value
    }

    private fun isCsrfTokenRequired(routing: RoutingContext, config: CsrfReactiveConfig): Boolean {
        return config.createTokenPath.map { it.contains(routing.normalizedPath()) }.orElse(true)
    }

    private fun createCookie(csrfToken: String?, routing: RoutingContext, config: CsrfReactiveConfig) {
        val cookie: ServerCookie = CookieImpl(config.cookieName, csrfToken)
        cookie.setHttpOnly(config.cookieHttpOnly)
        cookie.setSecure(config.cookieForceSecure || routing.request().isSSL)
        cookie.setMaxAge(config.cookieMaxAge.toSeconds())
        cookie.setPath(config.cookiePath)
        if (config.cookieDomain.isPresent) {
            cookie.setDomain(config.cookieDomain.get())
        }
        routing.response().addCookie(cookie)
    }

    private fun badClientRequest(): Response {
        return Response.status(400).build()
    }

    private fun requestMethodIsSafe(context: ContainerRequestContext): Boolean {
        return when (context.method) {
            "GET", "HEAD", "OPTIONS" -> true
            else -> false
        }
    }
}
