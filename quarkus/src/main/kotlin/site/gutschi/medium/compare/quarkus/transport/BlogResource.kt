package site.gutschi.medium.compare.quarkus.transport

import io.quarkus.hibernate.reactive.panache.common.WithSession
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ClientErrorException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.Response
import org.openapi.quarkus.openapi_yaml.api.BlogApi
import org.openapi.quarkus.openapi_yaml.model.*
import org.slf4j.LoggerFactory
import site.gutschi.medium.compare.quarkus.config.UserService
import site.gutschi.medium.compare.quarkus.db.Author
import site.gutschi.medium.compare.quarkus.db.AuthorRepository
import site.gutschi.medium.compare.quarkus.db.Post
import site.gutschi.medium.compare.quarkus.db.PostRepository
import java.math.BigDecimal
import java.time.LocalDateTime

@WithSession
class BlogResource(
    private val postRepository: PostRepository,
    private val authorRepository: AuthorRepository,
    private val userService: UserService
) : BlogApi {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getAllPosts(page: BigDecimal?): Uni<List<GenPostsInner>> {
        logger.debug("Get all Posts")
        return postRepository.findAll().list()
            .onItem().transform { list -> list.map { mapToGenPostsInner(it) } }
    }

    override fun getPost(id: String): Uni<GenPost> {
        logger.debug("Get Post {}", id)
        return postRepository.findById(id)
            .onItem().ifNull().failWith(NotFoundException())
            .onItem().transform { mapToGenPost(it) }
    }


    @WithTransaction
    override fun createOrUpdatePost(
        id: String,
        genPostUpdate: GenPostUpdate
    ): Uni<GenSuccessResponse> {
        logger.info("Create Or Update Post {} with {}", id, genPostUpdate)
        return postRepository.findById(id)
            .onItem().ifNotNull().transformToUni { it -> update(it, genPostUpdate) }
            .onItem().ifNull().switchTo { create(id, genPostUpdate) }
            .onItem().transform { GenSuccessResponse() }
    }


    private fun update(existingPost: Post, genPostUpdate: GenPostUpdate): Uni<Post> {
        ensureAllowToEdit(existingPost)
        existingPost.title = genPostUpdate.title ?: throw BadRequestException("title not defined")
        existingPost.content = genPostUpdate.content ?: throw BadRequestException("content not defined")
        existingPost.summary = genPostUpdate.summary ?: throw BadRequestException("summary not defined")
        existingPost.updated = LocalDateTime.now()
        logger.info("Updated post {} with {}", existingPost, genPostUpdate)
        return postRepository.persist(existingPost)
    }

    private fun create(id: String, genPostUpdate: GenPostUpdate): Uni<Post> {
        val post = Post()
        post.id = id
        post.content = genPostUpdate.content ?: throw BadRequestException("content not defined")
        post.title = genPostUpdate.title ?: throw BadRequestException("title not defined")
        post.summary = genPostUpdate.summary ?: throw BadRequestException("summary not defined")
        post.updated = LocalDateTime.now()
        post.created = LocalDateTime.now()
        val user = userService.getUser() ?: throw ClientErrorException(Response.Status.UNAUTHORIZED)
        return authorRepository.findById(user.id)
            .onItem().ifNull().switchTo {
                val author = Author()
                author.id = user.id
                author.firstname = user.firstname
                author.lastname = user.lastName
                logger.info("Insert author {}", author)
                authorRepository.persist(author)
            }.onItem().transformToUni { it ->
                post.author = it
                ensureAllowToEdit(post)
                logger.info("Created new post {}", post)
                postRepository.persist(post)
            }
    }

    @WithTransaction
    override fun deletePost(id: String): Uni<GenSuccessResponse> {
        logger.info("Delete Post {}", id)
        return postRepository.findById(id)
            .onItem().invoke(this::ensureAllowToEdit)
            .onItem().ifNull().failWith(NotFoundException())
            .onItem().transformToUni { _ -> postRepository.deleteById(id) }
            .onItem().transform { GenSuccessResponse().id(id) }
    }

    private fun mapToGenPost(post: Post): GenPost {
        return GenPost().id(post.id).title(post.title).summary(post.summary).content(post.content)
            .author(mapToGenAuthor(post.author)).created(post.created.toString())
            .updated(post.updated.toString())
    }

    private fun mapToGenPostsInner(post: Post): GenPostsInner {
        return GenPostsInner().id(post.id).title(post.title).summary(post.summary)
            .author(mapToGenAuthor(post.author)).created(post.created.toString())
            .updated(post.updated.toString())
    }

    private fun mapToGenAuthor(author: Author): GenAuthor {
        return GenAuthor().id(author.id).firstname(author.firstname).lastname(author.lastname)
    }


    private fun ensureAllowToEdit(post: Post) {
        val user = userService.getUser() ?: throw ClientErrorException(Response.Status.UNAUTHORIZED)
        if (user.roles.contains(UserService.Role.ADMIN)) {
            return
        }
        if (user.roles.contains(UserService.Role.WRITER) && post.author.id == user.id) {
            return
        }
        throw ForbiddenException()
    }
}