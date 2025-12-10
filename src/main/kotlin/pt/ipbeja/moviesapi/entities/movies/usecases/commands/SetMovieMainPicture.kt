package pt.ipbeja.moviesapi.entities.movies.usecases.commands

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.MoviePictureEntity
import pt.ipbeja.moviesapi.MoviePictures
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler


class SetMovieMainPictureCommand(val movieId: Int, val pictureId: Int): Request<Unit>

class SetMovieMainPictureCommandHandler(val db: Database) : RequestHandler<SetMovieMainPictureCommand, Unit> {



    override suspend fun handle(request: SetMovieMainPictureCommand) = transaction(db) {

        val newDefault = MoviePictureEntity.find { (MoviePictures.movie eq request.movieId) and (MoviePictures.id eq request.pictureId) }.forUpdate().firstOrNull() ?: throw Exception()
        val previousDefault = MoviePictureEntity.find { (MoviePictures.movie eq request.movieId) and (MoviePictures.mainPicture eq true) }.forUpdate().firstOrNull()

        previousDefault?.let {
            it.mainPicture = false
        }
        newDefault.mainPicture = true

    }



}