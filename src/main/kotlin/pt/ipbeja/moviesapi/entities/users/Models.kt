package pt.ipbeja.moviesapi.entities.users

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import pt.ipbeja.moviesapi.entities.movies.model.PictureInfo
import kotlin.time.Instant

@Serializable
enum class UserType {
    User,
    Admin
}


data class UserInfo(
    val id: Int,
    val role: String,
    val username: String,
    val dateOfBirth: LocalDate?,
    val picture : PictureInfo?,
    val createdAt: Instant,
    val updatedAt: Instant?
)


data class UserPictureInfo(val filename: String, val contentType: String, val path: String)