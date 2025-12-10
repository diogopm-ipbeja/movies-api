package pt.ipbeja.moviesapi.entities.genres.usecases.queries

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.GenreEntity
import pt.ipbeja.moviesapi.Genres
import pt.ipbeja.moviesapi.entities.genres.model.Genre
import pt.ipbeja.moviesapi.entities.genres.model.toGenre
import pt.ipbeja.moviesapi.entities.genres.model.toGenres
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler

class GetGenresQuery(val assignedOnly: Boolean = false) : Request<List<Genre>>

class GetGenresQueryHandler(val database: Database) : RequestHandler<GetGenresQuery, List<Genre>> {

    override suspend fun handle(request: GetGenresQuery): List<Genre> = transaction(database) {
        val all = GenreEntity.all().orderBy(Genres.name to SortOrder.ASC)
        if(!request.assignedOnly) {
            return@transaction all.toGenres()
        }

        all.filter {
            it.movies.count() > 0
        }.map { it.toGenre() }

    }

}