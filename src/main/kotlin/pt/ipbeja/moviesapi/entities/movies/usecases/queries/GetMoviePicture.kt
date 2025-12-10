package pt.ipbeja.moviesapi.entities.movies.usecases.queries

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.MoviePictureEntity
import pt.ipbeja.moviesapi.MoviePictures
import pt.ipbeja.moviesapi.entities.common.FileRepresentation
import pt.ipbeja.moviesapi.services.StorageService
import pt.ipbeja.moviesapi.utilities.*
import kotlin.io.path.div


class GetMoviePictureQuery(val movieId: Int, val pictureId: Int) : Request<ValueOr<FileRepresentation>>


class GetMoviePictureQueryHandler(val db: Database, val storageService: StorageService) :
    RequestHandler<GetMoviePictureQuery, ValueOr<FileRepresentation>> {
    override suspend fun handle(request: GetMoviePictureQuery) = transaction(db){

        val pic = MoviePictureEntity.Companion.find { (MoviePictures.movie eq request.movieId) and (MoviePictures.id eq request.pictureId) }
            .limit(1)
            .firstOrNull() ?: return@transaction ErrorI.Companion.notFound("Movie.PictureNotFound", "Picture not found").failure()


        val file = (storageService.baseDirectory / pic.url).toFile()
        if (!file.exists()) {
            throw Exception("Not found")
        }


        FileRepresentation(file, pic.filename, pic.contentType).success()
    }
}