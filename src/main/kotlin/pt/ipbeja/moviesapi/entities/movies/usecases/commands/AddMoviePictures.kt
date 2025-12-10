package pt.ipbeja.moviesapi.entities.movies.usecases.commands

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.MoviePictureEntity
import pt.ipbeja.moviesapi.MoviePictures
import pt.ipbeja.moviesapi.entities.common.CreatePicture
import pt.ipbeja.moviesapi.services.StorageService
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler

class AddMoviePicturesCommand(val movieId: Int, val pictures: List<CreatePicture>) : Request<Unit>

class AddMoviePicturesCommandHandler(val db: Database, val storageService: StorageService) : RequestHandler<AddMoviePicturesCommand, Unit> {

    override suspend fun handle(request: AddMoviePicturesCommand) = transaction(db) {
        val tx = storageService.createTransaction("movie_${request.movieId}")

        val filesWithPaths = request.pictures.map {
            it to tx.addFile(it.filename, byteArrayOf())
        }


        val newDefaultPicture = filesWithPaths.firstOrNull { it.first.mainPicture }
        if (newDefaultPicture != null) {
            val existingDefaultPic =
                MoviePictureEntity.Companion.find { (MoviePictures.movie eq request.movieId) and (MoviePictures.mainPicture eq true) }
                    .forUpdate()
                    .firstOrNull()
            existingDefaultPic?.apply {
                mainPicture = false
            }
        }

        MoviePictures.batchInsert(filesWithPaths) {
            this[MoviePictures.movie] = request.movieId
            this[MoviePictures.mainPicture] = it.first.mainPicture
            this[MoviePictures.filename] = it.first.filename
            this[MoviePictures.url] = it.second.toString()
        }

        tx.commit()

    }

}