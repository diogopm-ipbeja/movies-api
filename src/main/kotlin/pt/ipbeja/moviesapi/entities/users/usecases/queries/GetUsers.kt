package pt.ipbeja.moviesapi.entities.users.usecases.queries

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.UserEntity
import pt.ipbeja.moviesapi.entities.movies.model.PictureInfo
import pt.ipbeja.moviesapi.entities.users.UserInfo
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler
import pt.ipbeja.moviesapi.utilities.getMimeType


data object GetUsersQuery : Request<List<UserInfo>>

class GetUsersQueryHandler(val database: Database) : RequestHandler<GetUsersQuery, List<UserInfo>> {

    override suspend fun handle(request: GetUsersQuery): List<UserInfo> = transaction(database) {
        UserEntity.all()
            .map {
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
            ) }
    }

}