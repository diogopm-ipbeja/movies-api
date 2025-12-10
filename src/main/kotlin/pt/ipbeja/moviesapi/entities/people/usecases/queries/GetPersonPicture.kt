package pt.ipbeja.moviesapi.entities.people.usecases.queries

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.PersonPictureEntity
import pt.ipbeja.moviesapi.PersonPictures
import pt.ipbeja.moviesapi.entities.common.FileRepresentation
import pt.ipbeja.moviesapi.services.StorageService
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler
import kotlin.io.path.div


class GetPersonPictureQuery(val personId: Int, val pictureId: Int) : Request<FileRepresentation>


class GetPersonPictureQueryHandler(val db: Database, val storageService: StorageService) :
    RequestHandler<GetPersonPictureQuery, FileRepresentation> {
    override suspend fun handle(request: GetPersonPictureQuery) = transaction(db){
        val pic = PersonPictureEntity.Companion.find { (PersonPictures.person eq request.personId) and (PersonPictures.id eq request.pictureId) }
            .limit(1)
            .firstOrNull() ?: throw Exception()


        val file = (storageService.baseDirectory / pic.url).toFile()
        if (!file.exists()) {
            throw Exception("Not found")
        }


        FileRepresentation(file, pic.filename, pic.contentType)
    }
}