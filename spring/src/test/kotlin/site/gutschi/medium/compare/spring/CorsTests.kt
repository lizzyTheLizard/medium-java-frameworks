package site.gutschi.medium.compare.spring

import io.restassured.module.mockmvc.kotlin.extensions.Given
import io.restassured.module.mockmvc.kotlin.extensions.Then
import io.restassured.module.mockmvc.kotlin.extensions.When
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CorsTests {
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
    fun corsForLocalhost() {
        Given {
            mockMvc(mockMvc)
            header("Origin", "http://localhost:4200")
        } When {
            get("/api/posts")
        } Then {
            statusCode(200)
            header("Access-Control-Allow-Origin", "http://localhost:4200")
        }
    }

    @Test
    fun corsOther() {
        Given {
            mockMvc(mockMvc)
            header("Origin", "http://localhost2:4200")
        } When {
            get("/api/posts")
        } Then {
            statusCode(403)
            header("Access-Control-Allow-Origin", Matchers.nullValue())
        }
    }

    @Test
    fun sameHostNoOrigin() {
        Given {
            mockMvc(mockMvc)
        } When {
            get("/api/posts")
        } Then {
            statusCode(200)
            header("Access-Control-Allow-Origin", Matchers.nullValue())
        }
    }
}