package site.gutschi.medium.compare.spring

import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.ws.rs.core.HttpHeaders
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.openapi.quarkus.openapi_yaml.api.LoginApi


@QuarkusTest
@TestHTTPEndpoint(LoginApi::class)
class LoginTests {
    @Test
    fun notAuthenticated() {
        When {
            get("/login")
        } Then {
            statusCode(200)
            body("user", CoreMatchers.nullValue())
        }
    }

    @Test
    @TestSecurity(user = "testUser", roles = ["ADMIN", "IRRELEVANT"])
    fun authenticated() {
        When {
            get("/login")
        } Then {
            statusCode(200)
            body("user", CoreMatchers.notNullValue())
            body("user.id", Matchers.equalTo("testUser"))
            body("user.firstname", Matchers.equalTo(""))
            body("user.lastname", Matchers.equalTo(""))
            body("user.roles", Matchers.hasSize<String>(1))
            body("user.roles", Matchers.contains("ADMIN"))
        }
    }

    @Test
    fun redirectToAuthServerNonAuthenticated() {
        Given {
            redirects().follow(false)
        } When {
            queryParam("redirect", "http://localhost:4200/test")
            get("/start-login")
        } Then {
            statusCode(302)
            header(
                HttpHeaders.LOCATION,
                Matchers.startsWith("http://localhost:8090/auth/realms/blog/protocol/openid-connect/auth?response_type=code&client_id=blog&scope=openid&redirect_uri=http%3A%2F%2Flocalhost")
            )
        }
    }

    @Test
    @TestSecurity(user = "testUser", roles = ["ADMIN", "IRRELEVANT"], authorizationEnabled = false)
    fun okAuthenticated() {
        Given {
            redirects().follow(false)
        } When {
            queryParam("redirect", "http://localhost:4200/test")
            get("/start-login")
        } Then {
            statusCode(302)
            header(HttpHeaders.LOCATION, Matchers.startsWith("http://localhost:4200/test"))
        }
    }
}

