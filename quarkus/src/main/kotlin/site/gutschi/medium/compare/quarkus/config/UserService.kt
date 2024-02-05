package site.gutschi.medium.compare.quarkus.config

import io.quarkus.oidc.OidcSession
import io.quarkus.oidc.runtime.OidcJwtCallerPrincipal
import io.quarkus.security.identity.CurrentIdentityAssociation
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.jwt.Claims
import org.slf4j.LoggerFactory

@ApplicationScoped
class UserService(private val oidcSession: OidcSession) {
    enum class Role { ADMIN, WRITER }
    data class User(val id: String, val firstname: String, val lastName: String, val roles: List<Role>)

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val possibleRoles = Role.entries.map { it.name }

    fun getUser(): User? {
        val user = CurrentIdentityAssociation.current() ?: return null
        if (user.isAnonymous) return null
        val principal = user.principal
        if (principal is OidcJwtCallerPrincipal) {
            return User(
                id = principal.subject,
                firstname = principal.claims.getClaimValueAsString(Claims.given_name.name),
                lastName = principal.claims.getClaimValueAsString(Claims.family_name.name),
                roles = user.roles.mapNotNull { toRole(it) }
            )
        }
        logger.debug("Principal is of type {} which could be unusual", principal.javaClass.simpleName)
        return User(
            id = principal.name,
            firstname = "",
            lastName = "",
            roles = user.roles.mapNotNull { toRole(it) }
        )
    }

    private fun toRole(role: String): Role? {
        val roleUC = role.uppercase()
        if (possibleRoles.contains(roleUC)) {
            return Role.valueOf(roleUC)
        }
        return null
    }

    fun logout(): Uni<Void> {
        return this.oidcSession.logout()
    }
}