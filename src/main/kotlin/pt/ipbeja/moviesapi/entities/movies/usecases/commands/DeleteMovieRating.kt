package pt.ipbeja.moviesapi.entities.movies.usecases.commands

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.MovieRatingEntity
import pt.ipbeja.moviesapi.MovieRatings
import pt.ipbeja.moviesapi.Movies
import pt.ipbeja.moviesapi.Users
import pt.ipbeja.moviesapi.utilities.*

data class DeleteMovieRatingCommand(val movieId: Int, val userId: Int) : Request<ValueOr<Unit>>

class DeleteMovieRatingCommandHandler(val db: Database) : RequestHandler<DeleteMovieRatingCommand, ValueOr<Unit>> {
    override suspend fun handle(request: DeleteMovieRatingCommand) = transaction(db) {

        val exists = Movies.select(Movies.id).where { Movies.id eq request.movieId }.count() > 0
        if(!exists) return@transaction ErrorI.notFound("Movie.NotFound", "").failure()


        MovieRatingEntity.findById(CompositeID {
            it[MovieRatings.user] = EntityID(request.userId, Users)
            it[MovieRatings.movie] = EntityID(request.movieId, Movies)
        })?.delete()

        Unit.success()
    }
}

data class DeleteMovieRatingsCommand(val movieId: Int, val users: Set<Int>) : Request<ValueOr<Int>>

class DeleteMovieRatingsCommandHandler(val db: Database) : RequestHandler<DeleteMovieRatingsCommand, ValueOr<Int>> {
    override suspend fun handle(request: DeleteMovieRatingsCommand) = transaction(db) {

        val exists = Movies.select(Movies.id).where { Movies.id eq request.movieId }.count() > 0
        if(!exists) return@transaction ErrorI.notFound("Movie.NotFound", "").failure()


        val count = MovieRatings.deleteWhere { (MovieRatings.movie eq request.movieId) and (MovieRatings.user inList request.users) }

        count.success()
    }
}