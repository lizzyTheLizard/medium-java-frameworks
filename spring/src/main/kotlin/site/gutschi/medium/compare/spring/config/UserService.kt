package site.gutschi.medium.compare.spring.config

import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component

@Component
class UserService {
    enum class Role { ADMIN, WRITER }
    data class User(val id: String, val firstname: String, val lastName: String, val roles: List<Role>)

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val possibleRoles = Role.entries.map { it.name }

    fun getCurrentUser(): User? {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || authentication is AnonymousAuthenticationToken) {
            return null
        }
        if (authentication.principal is OidcUser) {
            val principal = authentication.principal as OidcUser
            val id = principal.attributes["sub"] as String
            val roles = authentication.authorities.mapNotNull { toRole(it) }
            return User(
                id = id,
                firstname = principal.attributes["given_name"] as String? ?: "",
                lastName = principal.attributes["family_name"] as String? ?: "",
                roles = roles
            )
        }
        if (authentication is UsernamePasswordAuthenticationToken) {
            logger.info("Principal is of type {} which could be unusual", authentication.principal.javaClass.simpleName)
            val roles = authentication.authorities.mapNotNull { toRole(it) }
            return User(
                id = authentication.name,
                firstname = "",
                lastName = "",
                roles = roles
            )
        }
        return null
    }

    private fun toRole(grantedAuthority: GrantedAuthority): Role? {
        val authority = grantedAuthority.authority
        val role = authority.uppercase().substringAfter("_")
        if (possibleRoles.contains(role)) {
            return Role.valueOf(role)
        }
        return null
    }
}