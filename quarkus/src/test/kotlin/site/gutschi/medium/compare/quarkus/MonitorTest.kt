package site.gutschi.medium.compare.quarkus

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.junit.jupiter.api.Test

@QuarkusTest
class MonitorTest {
    @Test
    fun health() {
        When {
            get("/q/health")
        } Then {
            statusCode(200)
            body("status", org.hamcrest.Matchers.equalTo("UP"))
        }
    }

    @Test
    fun metricsNotAllowedForAnonymous() {
        When {
            get("/q/metrics")
        } Then {
            statusCode(401)
        }
    }

    @Test
    @TestSecurity(user = "testUser", roles = ["admin"])
    fun metricsNotAllowedForInteractiveUser() {
        When {
            get("/q/metrics")
        } Then {
            statusCode(401)
        }
    }

    @Test
    fun metrics() {
        Given {
            auth().basic("admin", "admin")
        } When {
            get("/q/metrics")
        } Then {
            statusCode(200)
            body(org.hamcrest.Matchers.containsString("http_server_bytes_written_max"))
        }
    }
}