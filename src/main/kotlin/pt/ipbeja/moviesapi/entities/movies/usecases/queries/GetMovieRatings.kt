package pt.ipbeja.moviesapi.entities.movies.usecases.queries

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.MovieEntity
import pt.ipbeja.moviesapi.MovieRatingEntity
import pt.ipbeja.moviesapi.MovieRatings
import pt.ipbeja.moviesapi.utilities.*

/**
 * @author Diogo Pina Manique
 * @version 02/12/2025
 */

@Serializable
data class MovieRating(val movieId: Int, val userId: Int, val score: Int, val comment: String?)

class GetMovieRatingsQuery(val movieId: Int, val sortBy: String = "desc", val excludeUser: Int? = null) :
    Request<ValueOr<List<MovieRating>>>


class GetMovieRatingsQueryHandler(val db: Database) : RequestHandler<GetMovieRatingsQuery, ValueOr<List<MovieRating>>> {
    override suspend fun handle(request: GetMovieRatingsQuery) = transaction(db){

        val movie = MovieEntity.Companion.findById(request.movieId) ?: return@transaction ErrorI.Companion.notFound("Movie.NotFound", "Movie with id ${request.movieId} does not exist.").failure()

        val sortOrder = if(request.sortBy.equals("asc", true)) SortOrder.ASC else SortOrder.DESC

        val baseOp = MovieRatings.movie eq request.movieId
        val finalOp = if(request.excludeUser != null && request.excludeUser > 0) baseOp and (MovieRatings.user neq request.excludeUser) else baseOp

        MovieRatingEntity.Companion.find { finalOp }
            .orderBy(MovieRatings.id to sortOrder)
            .map {
                MovieRating(it.movieId.value, it.userId.value, it.score, it.comment)
            }.success()

    }
}


class GetMovieRatingQuery(val userId: Int, val movieId: Int) : Request<MovieRating?>


class GetMovieRatingQueryHandler(val db: Database) : RequestHandler<GetMovieRatingQuery, MovieRating?> {
    override suspend fun handle(request: GetMovieRatingQuery) = transaction(db){

        MovieRatingEntity.Companion.find { (MovieRatings.user eq request.userId) and (MovieRatings.movie eq request.movieId) }
            .limit(1)
            .map {
                MovieRating(it.movieId.value, it.userId.value, it.score, it.comment)
            }
            .firstOrNull()

    }
}

