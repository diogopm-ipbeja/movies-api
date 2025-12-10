package pt.ipbeja.moviesapi.entities.users.usecases.commands

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.UserEntity
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler

data class DeleteUserCommand(val userId: Int) : Request<Unit>


class DeleteUserCommandHandler(private val db: Database, private val masterAdmin: String) : RequestHandler<DeleteUserCommand, Unit> {
    override suspend fun handle(request: DeleteUserCommand) = transaction(db) {
        val user = UserEntity.findById(request.userId) ?: throw Exception("Not found")
        if (user.username == masterAdmin) throw Exception("Master admin cannot be deleted")
        user.delete()
    }
}