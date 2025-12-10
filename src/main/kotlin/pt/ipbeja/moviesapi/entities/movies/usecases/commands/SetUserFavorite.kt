package pt.ipbeja.moviesapi.entities.movies.usecases.commands

import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.FavoriteEntity
import pt.ipbeja.moviesapi.Favorites
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler


class SetUserFavoriteCommand(val movieId: Int, val userId: Int, val favorite: Boolean = true) : Request<Unit>

class SetUserFavoriteCommandHandler(private val db: Database) : RequestHandler<SetUserFavoriteCommand, Unit> {

    override suspend fun handle(request: SetUserFavoriteCommand) = transaction(db) {
        val compositeID = CompositeID {
            it[Favorites.user] = request.userId
            it[Favorites.movie] = request.movieId
        }

        val existingEntry = FavoriteEntity.findById(compositeID)
        if(!request.favorite) {
            existingEntry?.delete()
        }
        else if(existingEntry == null) {
            FavoriteEntity.new(compositeID) {}
        }
        Unit
    }
}