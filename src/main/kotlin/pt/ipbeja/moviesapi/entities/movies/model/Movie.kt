package pt.ipbeja.moviesapi.entities.movies.model

import kotlinx.datetime.LocalDate
import pt.ipbeja.moviesapi.entities.movies.Rating
import pt.ipbeja.moviesapi.entities.genres.model.Genre
import kotlin.time.Instant


data class MovieDetail(
    val id: Int,
    val title: String,
    val synopsis: String,
    val genres: Set<Genre>,
    val releaseDate: LocalDate,
    val director: Director?,
    val pictures: List<PictureInfo>,
    val rating: Rating?,
    val favorite: Boolean,
    val userRating: UserRating?,
    val minimumAge: Int,
    val createdAt: Instant,
    val updatedAt: Instant?,
    val cast: List<CastMember>,
)

data class MovieSimple(
    val id: Int,
    val title: String,
    val synopsis: String,
    val genres: Set<String>,
    val releaseDate: LocalDate,
    val mainPicture: PictureInfo?,
    val favorite: Boolean,
    val director: Director?,
    val rating: Float?,
    val createdAt: Instant,
    val updatedAt: Instant?,
)

data class CastMember(val personId: Int, val name: String, val character: String)

data class Director(val personId: Int, val name: String, val picture: PictureInfo?)

data class UserRating(val rating: Int, val comment: String?)