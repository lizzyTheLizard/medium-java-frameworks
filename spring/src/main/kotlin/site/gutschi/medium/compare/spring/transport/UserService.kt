package site.gutschi.medium.compare.spring.transport

import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component
import site.gutschi.medium.compare.spring.db.Role
import site.gutschi.medium.compare.spring.db.User

@Component
class UserService {
    val possibleRoles = Role.entries.map { it.name }

    fun getCurrentUser(): User? {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || authentication is AnonymousAuthenticationToken) {
            return null
        }
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

    private fun toRole(grantedAuthority: GrantedAuthority): Role? {
        val authority = grantedAuthority.authority
        val role = authority.uppercase().substringAfter("_")
        if (possibleRoles.contains(role)) {
            return Role.valueOf(role)
        }
        return null
    }
}