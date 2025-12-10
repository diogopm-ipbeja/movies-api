package pt.ipbeja.moviesapi.entities.users.usecases.commands

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.UserEntity
import pt.ipbeja.moviesapi.Users
import pt.ipbeja.moviesapi.entities.common.CreatePicture
import pt.ipbeja.moviesapi.entities.movies.model.PictureInfo
import pt.ipbeja.moviesapi.entities.users.UserInfo
import pt.ipbeja.moviesapi.services.StorageService
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler
import pt.ipbeja.moviesapi.utilities.getMimeType
import kotlin.io.path.pathString
import kotlin.time.Clock


data class SetUserPictureCommand(val id: Int, val picture: CreatePicture?) : Request<UserInfo>


class SetUserPictureCommandHandler(private val db: Database,
                                 private val storageService: StorageService) : RequestHandler<SetUserPictureCommand, UserInfo> {
    override suspend fun handle(request: SetUserPictureCommand): UserInfo = transaction(db){


        val user = UserEntity.find (Users.id eq request.id)
            .limit(1)
            .firstOrNull()

        if(user == null) throw Exception("not found")

        val tx = storageService.createTransaction("users")

        user.picture?.let {
            tx.removeFile(it)
        }

        val pic = request.picture
        val picInfo = if(pic != null) {
            val path = tx.addFile(pic.filename, pic.data)
            val picInfo = PictureInfo(user.id.value, pic.filename, pic.filename.getMimeType(), true, null)
            user.picture = path.pathString
            picInfo
        } else null

        user.updatedAt = Clock.System.now()

        tx.commit()
        UserInfo(user.id.value, user.role, user.username, user.dateOfBirth, picInfo, user.createdAt, user.updatedAt)

    }
}