package pt.ipbeja.moviesapi.entities.movies.usecases.commands

import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.*
import pt.ipbeja.moviesapi.entities.genres.model.Genre
import pt.ipbeja.moviesapi.entities.movies.model.CastMember
import pt.ipbeja.moviesapi.entities.movies.model.Director
import pt.ipbeja.moviesapi.entities.movies.model.MovieDetail
import pt.ipbeja.moviesapi.entities.movies.model.PictureInfo
import pt.ipbeja.moviesapi.services.StorageService
import pt.ipbeja.moviesapi.services.StorageTransaction
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler
import pt.ipbeja.moviesapi.utilities.getMimeType
import kotlin.time.Clock


data class CreateMoviesCommand(val movies: List<CreateMovieCommand>) : Request<List<MovieDetail>>

class CreateMoviesCommandHandler(val db: Database, val storageService: StorageService) : RequestHandler<CreateMoviesCommand, List<MovieDetail>> {


    override suspend fun handle(request: CreateMoviesCommand) = transaction(db) {

        val transactions = mutableListOf<StorageTransaction>()
        val results = request.movies.map { movie ->
            val defaultPicture = movie.pictures.firstOrNull { it.mainPicture} ?: movie.pictures.firstOrNull()

            val dirId = movie.directorId?.let { if(it > 0) it else null }
            val director = dirId?.let { PersonEntity[it]}

            val now = Clock.System.now()
            val movieId = Movies.insertAndGetId {
                movie.id?.let { providedId->
                    it[id] = providedId
                }
                it[title] = movie.title
                it[synopsis] = movie.synopsis
                it[releaseDate] = movie.releaseDate
                it[Movies.director] = director?.id
                it[createdAt] = now
            }


            val tx = storageService.createTransaction("movie_${movieId.value}")
            transactions.add(tx)
            val files = movie.pictures.map {
                it to tx.addFile(it.filename, it.data)
            }


            val moviePictures = MoviePictures.batchInsert(files) {
                this[MoviePictures.movie] = movieId
                this[MoviePictures.mainPicture] = it.first === defaultPicture
                this[MoviePictures.filename] = it.first.filename
                this[MoviePictures.contentType] = it.first.filename.getMimeType()
                this[MoviePictures.url] = it.second.toString()
                this[MoviePictures.description] = it.first.description
            }.map {
                PictureInfo(
                    it[MoviePictures.id].value,
                    it[MoviePictures.filename],
                    it[MoviePictures.contentType],
                    it[MoviePictures.mainPicture],
                    it[MoviePictures.description]
                )
            }

            val genres = Genres.selectAll().where { Genres.id inList movie.genres }.map { Genre(it[Genres.id].value, it[Genres.name], it[Genres.description]) }.toSet()

            MovieGenres.batchInsert(movie.genres) {
                this[MovieGenres.movie] = movieId
                this[MovieGenres.genre] = it
            }


            CastMembers.batchInsert(movie.cast) {
                this[CastMembers.person] = it.personId
                this[CastMembers.movie] = movieId
                this[CastMembers.role] = it.role
            }


            val people = movie.cast.map { it.personId }
            val cast = PersonEntity.find { Persons.id inList people }.toList()


            val castMembers = cast.map {
                val character = movie.cast.first { c -> c.personId == it.id.value }.role
                CastMember(it.id.value, it.name, character)
            }



            MovieDetail(
                movieId.value,
                movie.title,
                movie.synopsis,
                genres,
                movie.releaseDate,
                director?.let { Director(it.id.value, it.name, null)  },
                moviePictures,
                null,
                false,
                null,
                movie.minimumAge,
                now,
                null,
                castMembers
            )
        }

        transactions.forEach { it.commit() }

        return@transaction results

    }

}