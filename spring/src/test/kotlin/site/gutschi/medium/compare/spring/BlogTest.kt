package site.gutschi.medium.compare.spring

import io.restassured.module.mockmvc.kotlin.extensions.Given
import io.restassured.module.mockmvc.kotlin.extensions.Then
import io.restassured.module.mockmvc.kotlin.extensions.When
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import site.gutschi.medium.compare.spring.db.Author
import site.gutschi.medium.compare.spring.db.Post
import java.time.LocalDateTime
import java.util.*


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BlogTest {
    val csrfToken = "csrfToken"
    val cookieName = "XSRF-TOKEN"
    val tokenHeaderName = "X-XSRF-TOKEN"

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Test
    @WithMockUser(value = "testUser", roles = ["ADMIN"])
    fun createPost() {
        val mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        val post = Post(
            id = UUID.randomUUID().toString(),
            title = "title",
            summary = "summary",
            content = "content",
            author = Author(id = "testUser", firstname = "firstname", lastname = "lastname"),
            created = LocalDateTime.now(),
            updated = LocalDateTime.now()
        )

        Given {
            body(post)
            mockMvc(mockMvc)
            cookie(cookieName, csrfToken)
            header(tokenHeaderName, csrfToken)
            contentType("application/json")
        } When {
            put("/api/posts/" + post.id)
        } Then {
            statusCode(200)
        }

        Given {
            mockMvc(mockMvc)
        } When {
            get("/api/posts/" + post.id)
        } Then {
            statusCode(200)
            body("id", Matchers.equalTo(post.id))
            body("title", Matchers.equalTo(post.title))
            body("summary", Matchers.equalTo(post.summary))
            body("content", Matchers.equalTo(post.content))
            body("author.id", Matchers.equalTo(post.author.id))
        }

    }
}