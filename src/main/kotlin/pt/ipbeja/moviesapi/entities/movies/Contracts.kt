package pt.ipbeja.moviesapi.entities.movies

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import pt.ipbeja.moviesapi.entities.common.CreatePictureRequest
import pt.ipbeja.moviesapi.entities.common.PictureInfoResponse
import pt.ipbeja.moviesapi.entities.common.toResponse
import pt.ipbeja.moviesapi.entities.genres.GenreResponse
import pt.ipbeja.moviesapi.entities.genres.toResponse
import pt.ipbeja.moviesapi.entities.movies.model.CastMember
import pt.ipbeja.moviesapi.entities.movies.model.Director
import pt.ipbeja.moviesapi.entities.movies.model.MovieDetail
import pt.ipbeja.moviesapi.entities.movies.usecases.queries.MovieRating
import kotlin.time.Instant


@Serializable
data class CastMemberAssignmentRequest(val personId: Int, val character: String)


@Serializable
data class CreateMovieRequest(
    val title: String,
    val synopsis: String,
    val genres: Set<Int>,
    val releaseDate: LocalDate,
    val directorId: Int?,
    val minimumAge: Int = 0,
    val cast: List<CastMemberAssignmentRequest> = emptyList(),
    val pictures: List<CreatePictureRequest> = emptyList(),

)

@Serializable
data class UpdateMovieRequest(
    val id: Int,
    val title: String,
    val synopsis: String,
    val genres: List<Int>,
    val releaseDate: LocalDate,
    val directorId: Int?,
    val minimumAge: Int
)


@Serializable
data class MovieResponse(
    val id: Int,
    val title: String,
    val synopsis: String,
    val genres: Set<String>,
    val director: DirectorResponse,
    val minimumAge: Int,
    val releaseDate: LocalDate,
    val rating: Rating?,
    val mainPictureId: Int?,
    val createdAt: Instant,
    val updatedAt: Instant?
)

@Serializable
data class MovieRatingResponse(val score: Int, val comment: String?, val author: Int)

fun MovieRating.toResponse() = MovieRatingResponse(score, comment, userId)

@Serializable
data class MovieDetailResponse(
    val id: Int,
    val title: String,
    val synopsis: String,
    val genres: Set<GenreResponse>,
    val director: DirectorResponse?,
    val minimumAge: Int,
    val releaseDate: LocalDate,
    val rating: Rating?,
    val pictures: List<PictureInfoResponse>,
    val createdAt: Instant,
    val updatedAt: Instant?,
    val cast: List<CastMemberResponse>
)


@Serializable
data class MovieQueryResponse(
    val id: Int,
    val title: String,
    val synopsis: String,
    val genres: Set<String>,
    val releaseDate: LocalDate,
    val director: DirectorResponse?,
    val mainPicture: PictureInfoResponse?,
    val rating: Float?,
    val favorite: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant?
)




@Serializable
data class DirectorResponse(val personId: Int, val name: String, val picture: PictureInfoResponse?)

@Serializable
data class CastMemberResponse(val personId: Int, val name: String, val character: String)

fun MovieDetail.toResponse() = MovieDetailResponse(id, title, synopsis, genres.toResponse(), director?.toResponse(), minimumAge, releaseDate, rating, pictures.toResponse(), createdAt, updatedAt, cast.map { it.toResponse() })

fun Director.toResponse() =  DirectorResponse(personId, name, picture?.toResponse())






fun CastMember.toResponse() = CastMemberResponse(personId,name, character)
