package pt.ipbeja.moviesapi.entities.movies.usecases.commands

import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.select
import pt.ipbeja.moviesapi.MovieRatingEntity
import pt.ipbeja.moviesapi.MovieRatings
import pt.ipbeja.moviesapi.Movies
import pt.ipbeja.moviesapi.Persons
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler
import kotlin.time.Clock

data class RateMovieCommand(val movieId: Int, val userId: Int, val score: Int, val comment: String?) : Request<Unit>


class RateMovieCommandHandler(val db: Database) : RequestHandler<RateMovieCommand, Unit> {
    override suspend fun handle(request: RateMovieCommand) {

        val exists = Movies.select(Movies.id).where { Movies.id eq request.movieId }.count() > 0
        if(!exists) throw Exception("movie not found")

        MovieRatingEntity.findById(CompositeID {
            it[MovieRatings.user] = request.userId
            it[MovieRatings.movie] = request.movieId
        })?.apply {
            score = request.score
            comment = request.comment
            updatedAt = Clock.System.now()
        } ?: MovieRatingEntity.new {
            userId = EntityID(request.userId, Persons)
            movieId = EntityID(request.movieId, Movies)
            score = request.score
            comment = request.comment
        }

    }
}