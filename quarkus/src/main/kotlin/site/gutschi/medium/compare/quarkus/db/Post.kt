package site.gutschi.medium.compare.quarkus.db

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import java.time.LocalDateTime

@Entity
class Post {
    @Id
    lateinit var id: String
    lateinit var title: String
    lateinit var summary: String
    lateinit var content: String

    @ManyToOne(fetch = FetchType.EAGER)
    lateinit var author: Author
    lateinit var created: LocalDateTime
    lateinit var updated: LocalDateTime
}