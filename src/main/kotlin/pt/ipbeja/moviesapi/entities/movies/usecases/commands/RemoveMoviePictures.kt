package pt.ipbeja.moviesapi.entities.movies.usecases.commands

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteReturning
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.MoviePictures
import pt.ipbeja.moviesapi.services.StorageService
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler
import pt.ipbeja.moviesapi.utilities.ValueOr
import pt.ipbeja.moviesapi.utilities.success

data class RemoveMoviePicturesCommand(val movieId: Int, val pictureIds: Set<Int>) : Request<ValueOr<Int>>

class RemoveMoviePicturesCommandHandler(val db: Database, val storageService: StorageService) : RequestHandler<RemoveMoviePicturesCommand, ValueOr<Int>> {



    override suspend fun handle(request: RemoveMoviePicturesCommand): ValueOr<Int> = transaction(db) {
        val movieId = request.movieId
        val pictureIds = request.pictureIds
        val paths =
            MoviePictures.deleteReturning { (MoviePictures.movie eq movieId) and (MoviePictures.id inList pictureIds) }
                .map { it[MoviePictures.url] }

        val tx = storageService.createTransaction("movie_${request.movieId}")

        paths.forEach { tx.removeFile(it) }
        tx.commit()

        paths.count().success()
    }


}
