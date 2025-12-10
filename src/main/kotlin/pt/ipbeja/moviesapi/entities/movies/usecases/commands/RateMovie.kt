package pt.ipbeja.moviesapi.entities.movies.usecases.commands

import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.MovieRatingEntity
import pt.ipbeja.moviesapi.MovieRatings
import pt.ipbeja.moviesapi.Movies
import pt.ipbeja.moviesapi.Users
import pt.ipbeja.moviesapi.utilities.ErrorI
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler
import pt.ipbeja.moviesapi.utilities.ValueOr
import pt.ipbeja.moviesapi.utilities.failure
import pt.ipbeja.moviesapi.utilities.success
import kotlin.time.Clock

data class RateMovieCommand(val movieId: Int, val userId: Int, val score: Int, val comment: String?) : Request<ValueOr<Unit>>


class RateMovieCommandHandler(val db: Database) : RequestHandler<RateMovieCommand, ValueOr<Unit>> {
    override suspend fun handle(request: RateMovieCommand): ValueOr<Unit> = transaction(db) {
        // Validate score range
        if (request.score !in 0..5) {
            return@transaction ErrorI.validation("Rating.InvalidScore", "Score must be between 0 and 5").failure()
        }

        // Check if movie exists
        val movieExists = Movies.select(Movies.id).where { Movies.id eq request.movieId }.count() > 0
        if (!movieExists) {
            return@transaction ErrorI.notFound("Movie.NotFound", "Movie not found").failure()
        }

        // Check if user exists
        val userExists = Users.select(Users.id).where { Users.id eq request.userId }.count() > 0
        if (!userExists) {
            return@transaction ErrorI.notFound("User.NotFound", "User not found").failure()
        }

        val compositeId = CompositeID {
            it[MovieRatings.user] = request.userId
            it[MovieRatings.movie] = request.movieId
        }

        MovieRatingEntity.findById(compositeId)?.apply {
            score = request.score
            comment = request.comment
            updatedAt = Clock.System.now()
        } ?: MovieRatingEntity.new(compositeId) {
            score = request.score
            comment = request.comment
        }

        Unit.success()
    }
}