package pt.ipbeja.moviesapi.entities.users.usecases.commands

import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.UserEntity
import pt.ipbeja.moviesapi.Users
import pt.ipbeja.moviesapi.entities.common.CreatePicture
import pt.ipbeja.moviesapi.entities.movies.model.PictureInfo
import pt.ipbeja.moviesapi.entities.users.UserInfo
import pt.ipbeja.moviesapi.services.StorageService
import pt.ipbeja.moviesapi.utilities.ErrorI
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler
import pt.ipbeja.moviesapi.utilities.ValueOr
import pt.ipbeja.moviesapi.utilities.failure
import pt.ipbeja.moviesapi.utilities.getMimeType
import pt.ipbeja.moviesapi.utilities.hash
import pt.ipbeja.moviesapi.utilities.success
import kotlin.io.path.pathString

data class RegisterUserCommand(
    val username: String,
    val password: String,
    val role: String,
    val dateOfBirth: LocalDate?,
    val picture: CreatePicture?
) : Request<ValueOr<UserInfo>>


class RegisterUserCommandHandler(
    private val db: Database,
    private val storageService: StorageService
) : RequestHandler<RegisterUserCommand, ValueOr<UserInfo>> {
    override suspend fun handle(request: RegisterUserCommand): ValueOr<UserInfo> = transaction(db) {

        val count = UserEntity.count(Users.username eq request.username.trim())
        if(count > 0)  return@transaction ErrorI.conflict("User.Conflict", "Username already in use.").failure()

        val tx = request.picture?.let {
            val tx = storageService.createTransaction("users")
            val path = tx.addFile(it.filename, it.data)
            return@let tx to path
        }


        val userInfo = UserEntity.new {
            this.username = request.username.trim()
            this.passwordHash = request.password.hash()
            this.picture = tx?.second?.pathString
            this.role = request.role.trim().lowercase()
            this.dateOfBirth = request.dateOfBirth
        }.let {

            UserInfo(
                it.id.value,
                it.role,
                it.username,
                it.dateOfBirth,
                request.picture?.let { p ->
                    PictureInfo(it.id.value, p.filename, p.filename.getMimeType(), true, null)
                },
                it.createdAt,
                it.updatedAt
            )
        }

        tx?.first?.commit()

        userInfo.success()
    }
}


