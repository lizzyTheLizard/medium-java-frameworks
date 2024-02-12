package site.gutschi.medium.compare.spring

import io.restassured.module.mockmvc.kotlin.extensions.Given
import io.restassured.module.mockmvc.kotlin.extensions.Then
import io.restassured.module.mockmvc.kotlin.extensions.When
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
class MonitorTest {

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext
    lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .build()
    }

    @Test
    fun health() {
        Given {
            mockMvc(mockMvc)
        } When {
            get("/actuator/health")
        } Then {
            statusCode(200)
            body("status", org.hamcrest.Matchers.equalTo("UP"))
        }
    }

    @Test
    fun metricsNotAllowedForAnonymous() {
        Given {
            mockMvc(mockMvc)
        } When {
            get("/actuator/prometheus")
        } Then {
            statusCode(401)
        }
    }

    @Test
    @WithMockUser(value = "testUser", roles = ["ADMIN"])
    fun metricsNotAllowedForInteractiveUser() {
        Given {
            mockMvc(mockMvc)
        } When {
            get("/actuator/prometheus")
        } Then {
            statusCode(403)
        }
    }

    @Test
    fun metrics() {
        val auth = "admin:admin"
        val encodedAuth = Base64.getEncoder().encodeToString(auth.toByteArray(Charsets.UTF_8))
        Given {
            mockMvc(mockMvc)
            header(HttpHeaders.AUTHORIZATION, "Basic $encodedAuth")
        } When {
            get("/actuator/prometheus")
        } Then {
            statusCode(200)
            body(org.hamcrest.Matchers.containsString("hikaricp_connections_timeout_total"))
        }
    }
}