package site.gutschi.medium.compare.quarkus.db

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class PostRepository : PanacheRepositoryBase<Post, String>