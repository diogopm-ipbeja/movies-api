package pt.ipbeja.moviesapi.entities.genres.usecases.commands

import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.Genres
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler

/**
 * @author Diogo Pina Manique
 * @version 02/12/2025
 */

data class DeleteGenresCommand(val ids: Set<Int>) : Request<Int>

class DeleteGenresCommandHandler(private val db: Database) : RequestHandler<DeleteGenresCommand, Int> {
    override suspend fun handle(request: DeleteGenresCommand) = transaction(db){
        val result = Genres.deleteWhere { Genres.id inList request.ids }
        result
    }
}