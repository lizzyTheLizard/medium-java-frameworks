package site.gutschi.medium.compare.quarkus.db

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
class Author {
    @Id
    lateinit var id: String
    lateinit var firstname: String
    lateinit var lastname: String
}