package pt.ipbeja.moviesapi.entities.movies.usecases.commands

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.MovieEntity
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler

/**
 * @author Diogo Pina Manique
 * @version 09/12/2025
 */

data class DeleteMovieCommand(val id: Int) : Request<Unit>

class DeleteMovieCommandHandler(private val db: Database) : RequestHandler<DeleteMovieCommand, Unit>{
    override suspend fun handle(request: DeleteMovieCommand) = transaction(db) {
        MovieEntity.findById(request.id)?.delete() ?: throw Exception("Not found")
        Unit
    }
}