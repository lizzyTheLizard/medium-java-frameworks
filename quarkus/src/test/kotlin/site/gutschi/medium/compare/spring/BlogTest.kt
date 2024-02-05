package site.gutschi.medium.compare.spring

import io.quarkus.csrf.reactive.runtime.CsrfReactiveConfig
import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.openapi.quarkus.openapi_yaml.api.BlogApi
import site.gutschi.medium.compare.quarkus.db.Author
import site.gutschi.medium.compare.quarkus.db.Post
import java.time.LocalDateTime
import java.util.*


@QuarkusTest
@TestHTTPEndpoint(BlogApi::class)
class BlogTest {
    @Suppress("CdiInjectionPointsInspection")
    @Inject
    lateinit var config: CsrfReactiveConfig
    private final val csrfToken = "DUMMY_TOKEN_DUMMYTOKEN"

    @Test
    @TestSecurity(user = "testUser", roles = ["ADMIN"])
    fun createPost() {
        val post = Post()
        post.id = UUID.randomUUID().toString()
        post.title = "title"
        post.summary = "summary"
        post.content = "content"
        post.author = Author()
        post.author.id = "testUser"
        post.author.firstname = "firstname"
        post.author.lastname = "lastname"
        post.created = LocalDateTime.now()
        post.updated = LocalDateTime.now()

        When {
            get(post.id)
        } Then {
            statusCode(404)
        }

        Given {
            body(post)
            cookie(config.cookieName, csrfToken)
            header(config.tokenHeaderName, csrfToken)
            contentType("application/json")
        } When {
            put(post.id)
        } Then {
            statusCode(200)
        }

        When {
            get(post.id)
        } Then {
            statusCode(200)
            //body(".", Matchers.equalTo(post))
        }

    }
}