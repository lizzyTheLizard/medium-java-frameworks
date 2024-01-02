package site.gutschi.medium.compare.spring.transport

import org.openapitools.api.BlogApi
import org.openapitools.model.GenAuthor
import org.openapitools.model.GenPost
import org.openapitools.model.GenPostUpdate
import org.openapitools.model.GenPostsInner
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import site.gutschi.medium.compare.spring.db.Author
import site.gutschi.medium.compare.spring.db.Post
import site.gutschi.medium.compare.spring.db.PostsRepository
import site.gutschi.medium.compare.spring.db.Role
import java.math.BigDecimal
import java.time.LocalDateTime

@RestController
class PostsController(
    private val postsRepository: PostsRepository,
    private val userService: UserService
) : BlogApi {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getPost(id: String): ResponseEntity<GenPost> {
        logger.debug("Get Post {}", id)

        return postsRepository
            .findById(id)
            .map { mapToGenPost(it) }
            .map { ResponseEntity.ok(it) }
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
    }

    override fun getAllPosts(page: BigDecimal?): ResponseEntity<List<GenPostsInner>> {
        logger.debug("Get all Posts")

        val list = postsRepository.findAll()
            .map { mapToGenPostsInner(it) }
        return ResponseEntity.ok(list)
    }

    override fun deletePost(id: String): ResponseEntity<Unit> {
        logger.debug("Delete Post {}", id)

        postsRepository.findById(id)
            .ifPresentOrElse(
                { ensureAllowToEdit(it) },
                { throw ResponseStatusException(HttpStatus.NOT_FOUND) })
        postsRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    @Transactional
    override fun createOrUpdatePost(id: String, genPostUpdate: GenPostUpdate): ResponseEntity<Unit> {
        logger.debug("Create Or Update Post {}", id)
        postsRepository.findById(id).ifPresentOrElse({ update(it, genPostUpdate) }, { create(id, genPostUpdate) })
        return ResponseEntity.noContent().build()
    }

    private fun update(existing: Post, genPostUpdate: GenPostUpdate) {
        ensureAllowToEdit(existing)
        existing.title = genPostUpdate.title
        existing.content = genPostUpdate.content
        existing.summary = genPostUpdate.summary
        existing.updated = LocalDateTime.now()
        logger.info("Updated post {}", existing)
        postsRepository.save(existing)
    }

    private fun create(id: String, genPostUpdate: GenPostUpdate) {
        val post = Post(
            id = id,
            title = genPostUpdate.title,
            summary = genPostUpdate.summary,
            content = genPostUpdate.content,
            created = LocalDateTime.now(),
            updated = LocalDateTime.now(),
            author = getAuthor()
        )
        logger.info("Created new post {}", post)
        postsRepository.save(post)
    }

    private fun mapToGenPost(post: Post): GenPost {
        return GenPost(
            id = post.id,
            title = post.title,
            summary = post.summary,
            content = post.content,
            created = post.created.toString(),
            updated = post.updated.toString(),
            author = mapToGenAuthor(post.author)
        )
    }

    private fun mapToGenAuthor(author: Author): GenAuthor {
        return GenAuthor(id = author.id, firstname = author.firstname, lastname = author.lastname)
    }

    private fun mapToGenPostsInner(post: Post): GenPostsInner {
        return GenPostsInner(
            id = post.id,
            title = post.title,
            summary = post.summary,
            created = post.created.toString(),
            updated = post.updated.toString(),
            author = mapToGenAuthor(post.author)
        )
    }

    private fun getAuthor(): Author {
        val user = userService.getCurrentUser()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        return Author(id = user.id, firstname = user.firstname, lastname = user.lastName)
    }

    private fun ensureAllowToEdit(post: Post) {
        val user = userService.getCurrentUser()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        if (user.roles.contains(Role.ADMIN))
            return
        if (user.roles.contains(Role.WRITER) && post.author.id === user.id) {
            return
        }
        throw ResponseStatusException(HttpStatus.FORBIDDEN)
    }
}
