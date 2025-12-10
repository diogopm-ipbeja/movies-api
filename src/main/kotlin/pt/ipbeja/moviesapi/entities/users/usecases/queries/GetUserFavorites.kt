package pt.ipbeja.moviesapi.entities.users.usecases.queries

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.Favorites
import pt.ipbeja.moviesapi.UserEntity
import pt.ipbeja.moviesapi.entities.common.findMainPicture
import pt.ipbeja.moviesapi.entities.common.toPictureInfo
import pt.ipbeja.moviesapi.entities.movies.model.MovieSimple
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler


class GetUserFavoritesQuery(val id: Int) : Request<List<MovieSimple>?>

class GetUserFavoritesQueryHandler(val database: Database) : RequestHandler<GetUserFavoritesQuery, List<MovieSimple>?> {

    override suspend fun handle(request: GetUserFavoritesQuery): List<MovieSimple>? = transaction(database) {
        UserEntity.findById(request.id)?.let {
            it.favorites.orderBy(Favorites.createdAt to SortOrder.DESC)
                .map { m ->
                    val moviePicture = m.pictures.findMainPicture()
                    MovieSimple(
                        m.id.value,
                        m.title,
                        m.synopsis,
                        emptySet(),
                        m.releaseDate,
                        moviePicture?.toPictureInfo(),
                        true,
                        null,
                        null,
                        m.createdAt,
                        m.updatedAt
                    )
                }
        }
    }

}