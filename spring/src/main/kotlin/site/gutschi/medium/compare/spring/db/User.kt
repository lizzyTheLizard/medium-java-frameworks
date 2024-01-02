package site.gutschi.medium.compare.spring.db

data class User(
    val id: String,
    val firstname: String,
    val lastName: String,
    val roles: List<Role>
)