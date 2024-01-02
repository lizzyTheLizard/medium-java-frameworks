package site.gutschi.medium.compare.spring.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableMethodSecurity
class OidcConfig {
    @Bean
    fun oauth(http: HttpSecurity, corsConfiguration: CorsConfigurationSource): SecurityFilterChain {
        http.securityMatcher("/**")
        http.oauth2Login { it.userInfoEndpoint { it.userAuthoritiesMapper { it.flatMap { mapAuthorities(it) } } } }
        http.oidcLogout { it.backChannel {} }
        http.cors { it.configurationSource(corsConfiguration) }
        http.csrf {
            it.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            it.csrfTokenRequestHandler(CsrfTokenRequestAttributeHandler())
        }
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
        val blog = grantedAuthority.userInfo.getClaimAsMap("resource_access").get("blog") as Map<*, *>?
        val roles = blog?.get("roles") as List<*>? ?: return listOf(grantedAuthority)
        val authorities = roles.filterIsInstance<String>().map { SimpleGrantedAuthority("ROLE_$it") }
        return authorities.plus(grantedAuthority)
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
}
