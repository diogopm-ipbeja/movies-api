package pt.ipbeja.moviesapi.entities.people.usecases.commands

import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.Persons
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler

data class DeletePeopleCommand(val people: Set<Int>) : Request<Int>

class DeletePeopleCommandHandler(val db: Database) : RequestHandler<DeletePeopleCommand, Int> {
    override suspend fun handle(request: DeletePeopleCommand)= transaction(db) {
        Persons.deleteWhere { Persons.id inList request.people }
    }
}
