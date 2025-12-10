package pt.ipbeja.moviesapi.entities.users.usecases.commands

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.UserEntity
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler
import pt.ipbeja.moviesapi.utilities.hash

data class ChangeUserPasswordCommand(val userId: Int, val newPassword: String) : Request<Unit>


class ChangeUserPasswordCommandHandler(private val db: Database) : RequestHandler<ChangeUserPasswordCommand, Unit> {
    override suspend fun handle(request: ChangeUserPasswordCommand) = transaction(db) {
        UserEntity.findByIdAndUpdate(request.userId) {
            it.passwordHash = request.newPassword.hash()
        } ?: throw Exception("Not found")

        Unit
    }
}