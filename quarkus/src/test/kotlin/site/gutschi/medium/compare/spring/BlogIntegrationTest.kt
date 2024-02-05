package site.gutschi.medium.compare.spring

import io.quarkus.csrf.reactive.runtime.CsrfReactiveConfig
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheQuery
import io.quarkus.test.InjectMock
import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.openapi.quarkus.openapi_yaml.api.BlogApi
import site.gutschi.medium.compare.quarkus.db.Author
import site.gutschi.medium.compare.quarkus.db.Post
import site.gutschi.medium.compare.quarkus.db.PostRepository
import java.time.LocalDateTime


@QuarkusTest
@TestHTTPEndpoint(BlogApi::class)
class BlogIntegrationTest {
    @InjectMock
    lateinit var postRepository: PostRepository

    @Inject
    @Suppress("CdiInjectionPointsInspection")
    lateinit var config: CsrfReactiveConfig
    private final val csrfToken = "DUMMY_TOKEN_DUMMYTOKEN"

    @BeforeEach
    fun setup() {
        val post = createPost("id")
        val query = mock<PanacheQuery<Post>>()
        whenever(query.list()).thenReturn(Uni.createFrom().item(listOf(post)))
        reset(postRepository)
        whenever(postRepository.findAll()).thenReturn(query)
        whenever(postRepository.findById("id")).thenReturn(Uni.createFrom().item(post))
        whenever(postRepository.persist(any<Post>())).then { Uni.createFrom().item(it.arguments[0] as Post) }
    }

    @Test
    fun getPostsAnonymous() {
        When {
            get()
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
            cookie(config.cookieName, csrfToken)
            header(config.tokenHeaderName, csrfToken)
            contentType("application/json")
        } When {
            put("id2")
        } Then {
            statusCode(401)
        }

        verify(postRepository, never()).persist(any<Post>())
    }


    @Test
    @TestSecurity(user = "testUser", roles = ["ADMIN"])
    fun createPost() {
        val post = createPost("id2")

        Given {
            body(post)
            cookie(config.cookieName, csrfToken)
            header(config.tokenHeaderName, csrfToken)
            contentType("application/json")
        } When {
            put("id2")
        } Then {
            statusCode(200)
        }

        verify(postRepository).persist(argThat<Post> {
            id == "id2" && title == "title" && summary == "summary" && content == "content"
        })
    }

    @Test
    @TestSecurity(user = "wrongUser", roles = ["WRITER"])
    fun savePostWrongUser() {
        val post = createPost("id")
        Given {
            body(post)
            cookie(config.cookieName, csrfToken)
            header(config.tokenHeaderName, csrfToken)
            contentType("application/json")
        } When {
            put("id")
        } Then {
            statusCode(403)
        }
        verify(postRepository, never()).persist(any<Post>())
    }

    @Test
    @TestSecurity(user = "testUser", roles = ["WRITER"])
    fun savePostRightUser() {
        val post = createPost("id")

        Given {
            body(post)
            cookie(config.cookieName, csrfToken)
            header(config.tokenHeaderName, csrfToken)
            contentType("application/json")
        } When {
            put("id")
        } Then {
            statusCode(200)
        }

        verify(postRepository).persist(argThat<Post> {
            id == "id" && title == "title" && summary == "summary" && content == "content"
        })
    }


    @Test
    @TestSecurity(user = "wrongUser", roles = ["ADMIN"])
    fun savePostAdmin() {
        val post = createPost("id")

        Given {
            body(post)
            cookie(config.cookieName, csrfToken)
            header(config.tokenHeaderName, csrfToken)
            contentType("application/json")
        } When {
            put("id")
        } Then {
            statusCode(200)
        }

        verify(postRepository).persist(argThat<Post> {
            id == "id" && title == "title" && summary == "summary" && content == "content"
        })
    }

    fun createPost(id: String): Post {
        val post = Post()
        post.id = id
        post.title = "title"
        post.summary = "summary"
        post.content = "content"
        post.author = createAuthor()
        post.created = LocalDateTime.now()
        post.updated = LocalDateTime.now()
        return post
    }

    fun createAuthor(): Author {
        val author = Author()
        author.id = "testUser"
        author.firstname = "firstname"
        author.lastname = "lastname"
        return author
    }

}