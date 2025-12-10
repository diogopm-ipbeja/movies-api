package pt.ipbeja.moviesapi.entities.genres.usecases.commands

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.GenreEntity
import pt.ipbeja.moviesapi.Genres
import pt.ipbeja.moviesapi.entities.genres.model.Genre
import pt.ipbeja.moviesapi.entities.genres.model.toGenre
import pt.ipbeja.moviesapi.utilities.ErrorI
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler
import pt.ipbeja.moviesapi.utilities.ValueOr
import pt.ipbeja.moviesapi.utilities.failure
import pt.ipbeja.moviesapi.utilities.success


class UpdateGenreCommand(val id: Int, val name: String, val description: String?) : Request<ValueOr<Genre>>

class UpdateGenreCommandHandler(val database: Database) : RequestHandler<UpdateGenreCommand, ValueOr<Genre>> {

    override suspend fun handle(request: UpdateGenreCommand): ValueOr<Genre> = transaction(database) {

        val existing = GenreEntity.count(Genres.name eq request.name.trim())
        if(existing > 0) return@transaction ErrorI.conflict("Genre.Conflict", "Genre with same name already exists").failure()

        val genre = GenreEntity.findByIdAndUpdate(request.id) {
            it.name = request.name.trim()
            it.description = request.description?.trim()
        } ?:  return@transaction ErrorI.conflict("Genre.NotFound", "Genre with id '${request.id}' does not exist.").failure()

        genre.toGenre().success()
    }
}