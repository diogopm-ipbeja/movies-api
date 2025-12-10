package pt.ipbeja.moviesapi.entities.genres.usecases.commands

import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.Genres
import pt.ipbeja.moviesapi.entities.genres.model.Genre
import pt.ipbeja.moviesapi.entities.genres.model.toGenre
import pt.ipbeja.moviesapi.utilities.ErrorI
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler
import pt.ipbeja.moviesapi.utilities.ValueOr
import pt.ipbeja.moviesapi.utilities.failure
import pt.ipbeja.moviesapi.utilities.success


data class CreateGenre(val name: String, val description: String?)

data class CreateGenresCommand(val genres: List<CreateGenre>): Request<ValueOr<List<Genre>>>

class CreateGenresCommandHandler(val database: Database? = null) : RequestHandler<CreateGenresCommand, ValueOr<List<Genre>>> {


    override suspend fun handle(request: CreateGenresCommand): ValueOr<List<Genre>> = transaction(database) {

        val duplicates = request.genres.map { it.name }.groupBy { it }.filter { it.value.size > 1 }.map { it.key }
        if(duplicates.isNotEmpty()) throw Exception("duplicates found")

        val genreNames = request.genres.map { it.name.trim().lowercase() }


        val count = Genres.selectAll().where { Genres.name.lowerCase() inList genreNames}.count()
        if(count > 0) return@transaction ErrorI.conflict("Genres.Conflict", "One or more genres already exist.").failure()


        Genres.batchInsert(request.genres) {
            this[Genres.name] = it.name
            this[Genres.description] = it.description?.trim()
        }.map { it.toGenre() }.success()
    }

}