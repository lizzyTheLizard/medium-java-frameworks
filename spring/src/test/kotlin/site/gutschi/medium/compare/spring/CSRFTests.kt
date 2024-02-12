package site.gutschi.medium.compare.spring

import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.http.ContentType
import io.restassured.module.mockmvc.kotlin.extensions.Extract
import io.restassured.module.mockmvc.kotlin.extensions.Given
import io.restassured.module.mockmvc.kotlin.extensions.Then
import io.restassured.module.mockmvc.kotlin.extensions.When
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openapitools.model.GenPostUpdate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.*


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CSRFTests {
    val cookieName = "XSRF-TOKEN"
    val tokenHeaderName = "X-XSRF-TOKEN"
    private final val id = UUID.randomUUID()
    private final val ow = ObjectMapper().writer().withDefaultPrettyPrinter()
    private final val update = GenPostUpdate(id = id.toString(), summary = "sum", title = "title", content = "content")
    private final val body = ow.writeValueAsString(update)

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
    @WithMockUser(value = "testUser", roles = ["ADMIN"])
    fun csrf() {
        val csrfToken = Given {
            mockMvc(mockMvc)
        } When {
            get("/api/posts")
        } Then {
            statusCode(200)
            cookie(cookieName)
        } Extract {
            cookie(cookieName)
        }

        Given {
            mockMvc(mockMvc)
            cookie(cookieName, csrfToken)
            header(tokenHeaderName, csrfToken)
            body(body)
            contentType(ContentType.JSON)
        } When {
            put("/api/posts/$id")
        } Then {
            statusCode(200)
        }
    }

    @Test
    @WithMockUser(value = "testUser", roles = ["ADMIN"])
    fun wrongCsrf() {
        val csrfToken = Given {
            mockMvc(mockMvc)
        } When {
            get("/api/posts")
        } Then {
            statusCode(200)
            cookie(cookieName)
        } Extract {
            cookie(cookieName)
        }

        Given {
            mockMvc(mockMvc)
            cookie(cookieName, csrfToken)
            header(tokenHeaderName, "THIS IS WRONG")
            body(body)
            contentType(ContentType.JSON)
        } When {
            put("/api/posts/$id")
        } Then {
            statusCode(403)
        }
    }

    @Test
    @WithMockUser(value = "testUser", roles = ["ADMIN"])
    fun noCsrf() {
        Given {
            mockMvc(mockMvc)
            body(body)
            contentType(ContentType.JSON)
        } When {
            put("/api/posts/$id")
        } Then {
            statusCode(403)
        }
    }
}

