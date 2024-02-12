package site.gutschi.medium.compare.spring


import io.restassured.module.mockmvc.kotlin.extensions.Given
import io.restassured.module.mockmvc.kotlin.extensions.Then
import io.restassured.module.mockmvc.kotlin.extensions.When
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.aot.DisabledInAotMode
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import site.gutschi.medium.compare.spring.db.Author
import site.gutschi.medium.compare.spring.db.Post
import site.gutschi.medium.compare.spring.db.PostsRepository
import java.time.LocalDateTime
import java.util.*


//MockBean does not work in AOT mode, see https://github.com/spring-projects/spring-boot/issues/32195
@DisabledInAotMode
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BlogIntegrationTest {
    val csrfToken = "csrfToken"
    val cookieName = "XSRF-TOKEN"
    val tokenHeaderName = "X-XSRF-TOKEN"

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var postRepository: PostsRepository

    @BeforeEach
    fun setup() {
        val post = createPost("id")
        reset(postRepository)
        whenever(postRepository.findAll()).thenReturn(listOf(post))
        whenever(postRepository.findById("id")).thenReturn(Optional.of(post))
        whenever(postRepository.save(any<Post>())).then { it.arguments[0] as Post }
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun getPostsAnonymous() {
        Given {
            mockMvc(mockMvc)
        } When {
            get("/api/posts")
        } Then {
            statusCode(200)
            body("size()", Matchers.equalTo(1))
            body("[0].id", Matchers.equalTo("id"))
        }
    }

    @Test
    fun createPostAnonymous() {
        val post = createPost("id2")
        Given {
            body(post)
            mockMvc(mockMvc)
            cookie(cookieName, csrfToken)
            header(tokenHeaderName, csrfToken)
            contentType("application/json")
        } When {
            put("/api/posts/id2")
        } Then {
            statusCode(401)
        }

        verify(postRepository, never()).save(any<Post>())
    }

    @Test
    @WithMockUser(value = "testUser", roles = ["ADMIN"])
    fun createPost() {
        val post = createPost("id2")

        Given {
            body(post)
            mockMvc(mockMvc)
            cookie(cookieName, csrfToken)
            header(tokenHeaderName, csrfToken)
            contentType("application/json")
        } When {
            put("/api/posts/id2")
        } Then {
            statusCode(200)
        }

        verify(postRepository).save(argThat<Post> {
            id == "id2" && title == "title" && summary == "summary" && content == "content"
        })
    }

    @Test
    @WithMockUser(value = "wrongUser", roles = ["WRITER"])
    fun savePostWrongUser() {
        val post = createPost("id")
        Given {
            body(post)
            mockMvc(mockMvc)
            cookie(cookieName, csrfToken)
            header(tokenHeaderName, csrfToken)
            contentType("application/json")
        } When {
            put("/api/posts/id")
        } Then {
            statusCode(403)
        }
        verify(postRepository, never()).save(any<Post>())
    }

    @Test
    @WithMockUser(value = "testUser", roles = ["WRITER"])
    fun savePostRightUser() {
        val post = createPost("id")

        Given {
            body(post)
            mockMvc(mockMvc)
            cookie(cookieName, csrfToken)
            header(tokenHeaderName, csrfToken)
            contentType("application/json")
        } When {
            put("/api/posts/id")
        } Then {
            statusCode(200)
        }

        verify(postRepository).save(argThat<Post> {
            id == "id" && title == "title" && summary == "summary" && content == "content"
        })
    }


    @Test
    @WithMockUser(value = "wrongUser", roles = ["ADMIN"])
    fun savePostAdmin() {
        val post = createPost("id")

        Given {
            body(post)
            mockMvc(mockMvc)
            cookie(cookieName, csrfToken)
            header(tokenHeaderName, csrfToken)
            contentType("application/json")
        } When {
            put("/api/posts/id")
        } Then {
            statusCode(200)
        }

        verify(postRepository).save(argThat<Post> {
            id == "id" && title == "title" && summary == "summary" && content == "content"
        })
    }

    fun createPost(id: String): Post {
        return Post(
            id = id,
            title = "title",
            summary = "summary",
            content = "content",
            author = createAuthor(),
            created = LocalDateTime.now(),
            updated = LocalDateTime.now()
        )
    }

    fun createAuthor(): Author {
        return Author(id = "testUser", firstname = "firstname", lastname = "lastname")
    }
}