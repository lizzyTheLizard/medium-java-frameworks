package site.gutschi.medium.compare.quarkus

import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusTest
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.openapi.quarkus.openapi_yaml.api.BlogApi


@QuarkusTest
@TestHTTPEndpoint(BlogApi::class)
class CorsTests {
    @Test
    fun corsForLocalhost() {
        Given {
            header("Origin", "http://localhost:4200")
        } When {
            get()
        } Then {
            statusCode(200)
            header("Access-Control-Allow-Origin", "http://localhost:4200")
        }
    }

    @Test
    fun corsOther() {
        Given {
            header("Origin", "http://localhost2:4200")
        } When {
            get()
        } Then {
            statusCode(403)
            header("Access-Control-Allow-Origin", Matchers.nullValue())
        }
    }

    @Test
    fun sameHostNoOrigin() {
        When {
            get()
        } Then {
            statusCode(200)
            header("Access-Control-Allow-Origin", Matchers.nullValue())
        }
    }


}