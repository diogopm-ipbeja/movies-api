package pt.ipbeja.moviesapi.entities.movies.usecases.commands

import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.*
import pt.ipbeja.moviesapi.entities.common.CreatePicture
import pt.ipbeja.moviesapi.entities.genres.model.Genre
import pt.ipbeja.moviesapi.entities.movies.model.*
import pt.ipbeja.moviesapi.services.StorageService
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler
import pt.ipbeja.moviesapi.utilities.getMimeType
import kotlin.time.Clock


data class CreateMovieCommand(
    val title: String,
    val synopsis: String,
    val cast: List<RoleAssignment>,
    val pictures: List<CreatePicture>,
    val genres: Set<Int>,
    val directorId: Int?,
    val releaseDate: LocalDate,
    val minimumAge: Int,
    val id: Int? = null
) : Request<MovieDetail>


class CreateMovieCommandHandler(val db: Database, val storageService: StorageService) : RequestHandler<CreateMovieCommand, MovieDetail> {


    override suspend fun handle(request: CreateMovieCommand) = transaction(db) {



        val defaultPicture = request.pictures.firstOrNull { it.mainPicture} ?: request.pictures.firstOrNull()

        val dirId = request.directorId?.let { if(it > 0) it else null }
        val director = dirId?.let { PersonEntity[it]}

        val now = Clock.System.now()
        val movieId = Movies.insertAndGetId {
            it[title] = request.title
            it[synopsis] = request.synopsis
            it[releaseDate] = request.releaseDate
            it[minimumAge] = request.minimumAge
            it[Movies.director] = director?.id
            it[createdAt] = now
        }

        val tx = storageService.createTransaction("movie_${movieId.value}")
        val files = request.pictures.map {
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

        val genres = Genres.selectAll().where { Genres.id inList request.genres }.map { Genre(it[Genres.id].value, it[Genres.name], it[Genres.description]) }.toSet()

        MovieGenres.batchInsert(request.genres) {
            this[MovieGenres.movie] = movieId
            this[MovieGenres.genre] = it
        }


        CastMembers.batchInsert(request.cast) {
            this[CastMembers.person] = it.personId
            this[CastMembers.movie] = movieId
            this[CastMembers.role] = it.role
        }


        val people = request.cast.map { it.personId }
        val cast = PersonEntity.find { Persons.id inList people }.toList()


        val castMembers = cast.map {
            val character = request.cast.first { c -> c.personId == it.id.value }.role
            CastMember(it.id.value, it.name, character)
        }

        tx.commit()

        // val directorPic = director.pictures.toList().let {
        //     it.firstOrNull { d -> d.mainPicture } ?: it.firstOrNull()
        // }
        MovieDetail(
            movieId.value,
            request.title,
            request.synopsis,
            genres,
            request.releaseDate,
            director?.let { Director(it.id.value, it.name, null)  },
            moviePictures,
            null,
            false,
            null,
            request.minimumAge,
            now,
            null,
            castMembers
        )

    }

}