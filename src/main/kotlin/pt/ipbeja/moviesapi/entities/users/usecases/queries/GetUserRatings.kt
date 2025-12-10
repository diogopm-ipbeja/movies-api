package pt.ipbeja.moviesapi.entities.users.usecases.queries

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.MovieRatings
import pt.ipbeja.moviesapi.UserEntity
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler

data class UserRating(val movieId: Int, val movieTitle: String, val score: Int, val comment: String?)

class GetUserRatingsQuery(val id: Int) : Request<List<UserRating>?>

class GetUserRatingsQueryHandler(val database: Database) : RequestHandler<GetUserRatingsQuery, List<UserRating>?> {

    override suspend fun handle(request: GetUserRatingsQuery): List<UserRating>? = transaction(database) {
        UserEntity.findById(request.id)?.let {
            it.ratedMovies.orderBy(MovieRatings.createdAt to SortOrder.DESC)
                .map { mr ->
                UserRating(mr.movieId.value, mr.movie.title, mr.score, mr.comment)
            }
        }
    }

}