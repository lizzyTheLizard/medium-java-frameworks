package site.gutschi.medium.compare.quarkus.transport

import io.quarkus.security.Authenticated
import io.smallrye.mutiny.Uni
import jakarta.ws.rs.RedirectionException
import org.openapi.quarkus.openapi_yaml.api.LoginApi
import org.openapi.quarkus.openapi_yaml.model.GenLogin
import org.openapi.quarkus.openapi_yaml.model.GenSuccessResponse
import org.openapi.quarkus.openapi_yaml.model.GenUser
import org.slf4j.LoggerFactory
import site.gutschi.medium.compare.quarkus.config.UserService
import java.net.URI


class LoginResource(private val userService: UserService) : LoginApi {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getUser(): Uni<GenLogin> {
        val user = userService.getUser()
        logger.debug("Current user is {}", user)
        return Uni.createFrom().item(GenLogin().user(if (user != null) mapToGenUser(user) else null))
    }

    private fun mapToGenUser(user: UserService.User): GenUser {
        return GenUser().id(user.id).firstname(user.firstname).lastname(user.lastName).roles(user.roles.map { it.name })
    }

    @Authenticated
    override fun login(redirect: String?): Uni<GenSuccessResponse> {
        val uri = URI(redirect ?: "/")
        val user = userService.getUser()
        logger.info("User {} is now logged in, redirect to {}", user, redirect)
        throw RedirectionException(302, uri)
    }

    override fun logout(): Uni<GenSuccessResponse> {
        val user = userService.getUser()
        return userService.logout()
            .onItem().invoke { _ -> logger.info("User {} is not logged out", user) }
            .replaceWith(GenSuccessResponse())
    }

}