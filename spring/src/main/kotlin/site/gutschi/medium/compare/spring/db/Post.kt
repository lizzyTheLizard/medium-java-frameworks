package site.gutschi.medium.compare.spring.db

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
data class Post(
    @Id val id: String,
    var title: String,
    var summary: String,
    var content: String,
    @ManyToOne(fetch = FetchType.EAGER, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    val author: Author,
    val created: LocalDateTime,
    var updated: LocalDateTime
)