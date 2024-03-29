package site.gutschi.medium.compare.spring.transport

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.openapitools.api.LoginApi
import org.openapitools.model.GenLogin
import org.openapitools.model.GenSuccessResponse
import org.openapitools.model.GenUser
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.web.bind.annotation.RestController
import site.gutschi.medium.compare.spring.config.UserService


@RestController
class LoginController(
    private val userService: UserService,
    private val request: HttpServletRequest,
    private val response: HttpServletResponse
) : LoginApi {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getUser(): ResponseEntity<GenLogin> {
        val user = userService.getCurrentUser()
        val response = if (user == null) GenLogin() else GenLogin(user = mapToGenUser(user))
        return ResponseEntity.ok(response)
    }

    override fun login(redirect: String?): ResponseEntity<GenSuccessResponse> {
        val headers = HttpHeaders()
        headers.add("Location", redirect)
        return ResponseEntity<GenSuccessResponse>(headers, HttpStatus.FOUND)
    }

    override fun logout(): ResponseEntity<GenSuccessResponse> {
        logger.debug("Start logout")
        val authentication = SecurityContextHolder.getContext().authentication
        val logoutHandler = SecurityContextLogoutHandler()
        logoutHandler.logout(request, response, authentication)
        val response = GenSuccessResponse()
        return ResponseEntity.ok(response)
    }

    private fun mapToGenUser(user: UserService.User): GenUser {
        return GenUser(
            id = user.id,
            firstname = user.firstname,
            lastname = user.lastName,
            roles = user.roles.map { it.name }
        )
    }

}