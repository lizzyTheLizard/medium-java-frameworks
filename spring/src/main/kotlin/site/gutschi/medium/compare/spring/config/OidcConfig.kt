package site.gutschi.medium.compare.spring.config

import org.springframework.boot.autoconfigure.security.SecurityProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.util.StringUtils
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource


@Configuration
@EnableMethodSecurity
class OidcConfig {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 9)
    fun basicAuth(http: HttpSecurity, corsConfiguration: CorsConfigurationSource): SecurityFilterChain {
        http.securityMatcher("/actuator/**")
        http.httpBasic { }
        http.cors { it.configurationSource(corsConfiguration) }
        configureCsrf(http)
        http.authorizeHttpRequests {
            it.requestMatchers("/actuator/prometheus").hasRole("METRICS")
            it.anyRequest().permitAll()
        }
        return http.build()
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    fun oauth(http: HttpSecurity, corsConfiguration: CorsConfigurationSource): SecurityFilterChain {
        http.securityMatcher("/**")
        http.oauth2Login { c -> c.userInfoEndpoint { c2 -> c2.userAuthoritiesMapper { a -> a.flatMap { mapAuthorities(it) } } } }
        http.oidcLogout { it.backChannel {} }
        http.cors { it.configurationSource(corsConfiguration) }
        configureCsrf(http)
        http.authorizeHttpRequests {
            it.requestMatchers("/api/start-login").authenticated()
            it.anyRequest().permitAll()
        }
        return http.build()
    }

    private fun mapAuthorities(grantedAuthority: GrantedAuthority): Collection<GrantedAuthority> {
        if (grantedAuthority !is OidcUserAuthority) {
            return listOf(grantedAuthority)
        }
        if (!grantedAuthority.userInfo.hasClaim("resource_access")) {
            return listOf(grantedAuthority)
        }
        val blog = grantedAuthority.userInfo.getClaimAsMap("resource_access")["blog"] as Map<*, *>?
        val roles = blog?.get("roles") as List<*>? ?: return listOf(grantedAuthority)
        val authorities = roles.filterIsInstance<String>().map { SimpleGrantedAuthority("ROLE_$it") }
        return authorities.plus(grantedAuthority)
    }

    private fun configureCsrf(http: HttpSecurity) {
        //always send csrf cookie, see https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#deferred-csrf-token
        val csrfTokenRequestHandler = CsrfTokenRequestAttributeHandler()
        csrfTokenRequestHandler.setCsrfRequestAttributeName(null)

        http.csrf {
            it.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            it.csrfTokenRequestHandler(csrfTokenRequestHandler)
        }
    }

    @Bean
    fun corsConfiguration(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("http://localhost:4200")
        configuration.setAllowedMethods(listOf("*"))
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun userDetailsService(properties: SecurityProperties): UserDetailsService {
        //Copied from UserDetailsServiceAutoConfiguration. utoConfig get sipped as also Oauth2 is on the classpath
        val user: SecurityProperties.User = properties.user
        val roles = user.roles
        return InMemoryUserDetailsManager(
            User.withUsername(user.name)
                .password(user.password)
                .roles(*StringUtils.toStringArray(roles))
                .build()
        )
    }


}
