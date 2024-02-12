package site.gutschi.medium.compare.spring

import io.restassured.module.mockmvc.kotlin.extensions.Given
import io.restassured.module.mockmvc.kotlin.extensions.Then
import io.restassured.module.mockmvc.kotlin.extensions.When
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoginTests {

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
    fun notAuthenticated() {
        Given {
            mockMvc(mockMvc)
        } When {
            get("/api/login")
        } Then {
            statusCode(200)
            body("user", CoreMatchers.nullValue())
        }
    }

    @Test
    @WithMockUser(value = "testUser", roles = ["ADMIN"])
    fun authenticated() {
        Given {
            mockMvc(mockMvc)
        } When {
            get("/api/login")
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
            mockMvc(mockMvc)
            queryParam("redirect", "http://localhost:4200/test")
        } When {
            get("/api/start-login")
        } Then {
            statusCode(302)
            header(HttpHeaders.LOCATION, Matchers.startsWith("http://localhost/oauth2/authorization/idp"))
        }
    }

    @Test
    @WithMockUser(value = "testUser", roles = ["ADMIN"])
    fun okAuthenticated() {
        Given {
            mockMvc(mockMvc)
            queryParam("redirect", "http://localhost:4200/test")
        } When {
            get("/api/start-login")
        } Then {
            statusCode(302)
            header(HttpHeaders.LOCATION, Matchers.startsWith("http://localhost:4200/test"))
        }
    }
}

