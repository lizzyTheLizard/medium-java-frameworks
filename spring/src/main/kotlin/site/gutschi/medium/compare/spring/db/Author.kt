package site.gutschi.medium.compare.spring.db

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
data class Author(
    @Id val id: String,
    val firstname: String,
    val lastname: String
)