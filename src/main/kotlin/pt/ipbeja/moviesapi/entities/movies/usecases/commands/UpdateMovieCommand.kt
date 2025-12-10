package pt.ipbeja.moviesapi.entities.movies.usecases.commands

import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.*
import pt.ipbeja.moviesapi.entities.common.findMainPicture
import pt.ipbeja.moviesapi.entities.common.toPictureInfo
import pt.ipbeja.moviesapi.entities.movies.model.Director
import pt.ipbeja.moviesapi.entities.movies.model.MovieSimple
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler
import java.math.RoundingMode
import kotlin.time.Clock

fun <T> Iterable<T>.exceptBy(
    other: Iterable<T>,
    predicate: ((T, T) -> Boolean)
): List<T> = this.filterNot { a ->
    other.any { b ->
        predicate(a, b)
    }
}


data class UpdateMovieCommand(
    val id: Int,
    val title: String,
    val synopsis: String,
    val genres: Set<Int>,
    val directorId: Int,
    val releaseDate: LocalDate,
    val minimumAge: Int = 0
) : Request<MovieSimple>


data class UpdatedMovie(val id: Int)


class UpdateMovieCommandHandler(val db: Database) : RequestHandler<UpdateMovieCommand, MovieSimple> {



    override suspend fun handle(request: UpdateMovieCommand) = transaction(db) {

        val movie = MovieEntity.Companion.findById(request.id)?.apply {
            title = request.title
            synopsis = request.synopsis
            directorId = EntityID(request.directorId, Persons)
            releaseDate = request.releaseDate
            updatedAt = Clock.System.now()
            minimumAge = request.minimumAge
        } ?: throw Exception("Not found")




        val avgCol = MovieRatings.score.avg().alias("rating_average")
        val avgRating = MovieRatings.select(MovieRatings.movie, avgCol)
                .where { MovieRatings.movie eq request.id}
                .map { it[avgCol] }
                .firstOrNull()?.setScale(2, RoundingMode.HALF_UP)?.toFloat()


        val existingGenres = Genres.select(Genres.id).map { it[Genres.id] }.toSet()
        val nonExistentGenres = request.genres - existingGenres
        if(nonExistentGenres.isNotEmpty()) throw Exception("Genres don't exist")


        val currentGenreIds = MovieGenres
            .selectAll()
            .where { MovieGenres.movie eq request.id }.map { it[MovieGenres.genre].value }
            .toSet()

        val remaining = request.genres.intersect(currentGenreIds)
        val removed = currentGenreIds - remaining
        val added = remaining - currentGenreIds


        MovieGenres.deleteIgnoreWhere { (MovieGenres.movie eq request.id) and (MovieGenres.genre inList removed) }
        MovieGenres.batchInsert(added) {
            this[MovieGenres.movie] = request.id
            this[MovieGenres.genre] = it
        }


        val mainPic = movie.pictures.findMainPicture()

        val director = movie.director?.let {
            Director(it.id.value, it.name, it.pictures.findMainPicture()?.toPictureInfo())
        }
        MovieSimple(movie.id.value, movie.title, movie.synopsis, movie.genres.map { it.name }.toSet(), movie.releaseDate, mainPic?.toPictureInfo(), false, director, avgRating, movie.createdAt, movie.updatedAt)

    }



}




