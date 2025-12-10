package pt.ipbeja.moviesapi.entities.users.usecases.queries

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.UserEntity
import pt.ipbeja.moviesapi.entities.common.FileRepresentation
import pt.ipbeja.moviesapi.entities.movies.model.PictureInfo
import pt.ipbeja.moviesapi.entities.users.UserInfo
import pt.ipbeja.moviesapi.services.StorageService
import pt.ipbeja.moviesapi.utilities.ErrorI
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler
import pt.ipbeja.moviesapi.utilities.ValueOr
import pt.ipbeja.moviesapi.utilities.failure
import pt.ipbeja.moviesapi.utilities.getMimeType
import pt.ipbeja.moviesapi.utilities.success
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.notExists


class GetUserQuery(val id: Int) : Request<ValueOr<UserInfo>>

class GetUserQueryHandler(val database: Database) : RequestHandler<GetUserQuery, ValueOr<UserInfo>> {

    override suspend fun handle(request: GetUserQuery): ValueOr<UserInfo> = transaction(database) {
        val userEntity = UserEntity.findById(request.id) ?: return@transaction ErrorI.notFound(
            "User.NotFound",
            "User '${request.id}' does not exist."
        ).failure()

        userEntity.let {
            val picture = it.picture?.let { pic ->
                PictureInfo(0, pic, pic.getMimeType(), true, null)
            }
            UserInfo(
                it.id.value,
                it.role,
                it.username,
                it.dateOfBirth,
                picture,
                it.createdAt,
                it.updatedAt
            )
        }.success()

    }
}

class GetUserPictureQuery(val id: Int) : Request<ValueOr<FileRepresentation>>

class GetUserPictureQueryHandler(val database: Database, val storageService: StorageService) :
    RequestHandler<GetUserPictureQuery, ValueOr<FileRepresentation>> {

    override suspend fun handle(request: GetUserPictureQuery): ValueOr<FileRepresentation> = transaction(database) {

        val userEntity = UserEntity.findById(request.id) ?: return@transaction ErrorI.notFound(
            "User.NotFound",
            "User '${request.id}' does not exist."
        ).failure()


        userEntity.let {
            val pic = it.picture?.let { pic ->
                // val picture = PictureInfo(0, pic, pic.getMimeType(), true, null)
                val path = storageService.baseDirectory / pic
                if(path.notExists()) return@let null
                FileRepresentation(
                    path.toFile(),
                    path.fileName.name,
                    path.fileName.name.getMimeType()
                )
            }

            pic
        }?.success() ?: ErrorI.notFound("User.PictureNotFound", "User '${request.id}' has no picture.").failure()
    }
}