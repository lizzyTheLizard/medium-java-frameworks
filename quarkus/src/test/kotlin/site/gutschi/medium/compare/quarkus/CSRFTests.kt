package site.gutschi.medium.compare.quarkus

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.csrf.reactive.runtime.CsrfReactiveConfig
import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.openapi.quarkus.openapi_yaml.api.BlogApi
import org.openapi.quarkus.openapi_yaml.model.GenPostUpdate
import java.util.*


@QuarkusTest
@TestHTTPEndpoint(BlogApi::class)
class CSRFTests {
    private final val id = UUID.randomUUID()
    private final val ow = ObjectMapper().writer().withDefaultPrettyPrinter()
    private final val update = GenPostUpdate().id(id.toString()).summary("sum").title("title").content("content")
    private final val body = ow.writeValueAsString(update)

    @Inject
    @Suppress("CdiInjectionPointsInspection")
    lateinit var config: CsrfReactiveConfig

    @Test
    @TestSecurity(user = "testUser", roles = ["ADMIN"])
    fun csrf() {
        val csrfToken = When {
            get()
        } Then {
            statusCode(200)
            cookie(config.cookieName)
        } Extract {
            cookie(config.cookieName)
        }

        Given {
            cookie(config.cookieName, csrfToken)
            header(config.tokenHeaderName, csrfToken)
            body(body)
            contentType(ContentType.JSON)
        } When {
            put(id.toString())
        } Then {
            statusCode(200)
        }
    }

    @Test
    @TestSecurity(user = "testUser", roles = ["ADMIN"])
    fun wrongCsrf() {
        val csrfToken = When {
            get()
        } Then {
            statusCode(200)
            cookie(config.cookieName)
        } Extract {
            cookie(config.cookieName)
        }

        Given {
            cookie(config.cookieName, csrfToken)
            header(config.tokenHeaderName, "THIS IS WRONG")
            body(body)
            contentType(ContentType.JSON)
        } When {
            put(id.toString())
        } Then {
            statusCode(400)
        }
    }

    @Test
    @TestSecurity(user = "testUser", roles = ["ADMIN"])
    fun noCsrf() {
        Given {
            body(body)
            contentType(ContentType.JSON)
        } When {
            put(id.toString())
        } Then {
            statusCode(400)
        }
    }
}

