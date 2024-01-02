package site.gutschi.medium.compare.spring.db

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PostsRepository : CrudRepository<Post, String>