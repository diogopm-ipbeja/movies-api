package pt.ipbeja.moviesapi.entities.movies

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.getOrFail
import io.ktor.util.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import pt.ipbeja.moviesapi.entities.common.CreatePicture
import pt.ipbeja.moviesapi.entities.common.FileRepresentation
import pt.ipbeja.moviesapi.entities.common.toResponse
import pt.ipbeja.moviesapi.entities.movies.model.RoleAssignment
import pt.ipbeja.moviesapi.entities.movies.usecases.commands.*
import pt.ipbeja.moviesapi.entities.movies.usecases.queries.GetMoviePictureQuery
import pt.ipbeja.moviesapi.entities.movies.usecases.queries.GetMoviePictureQueryHandler
import pt.ipbeja.moviesapi.entities.movies.usecases.queries.GetMovieQuery
import pt.ipbeja.moviesapi.entities.movies.usecases.queries.GetMovieQueryHandler
import pt.ipbeja.moviesapi.entities.movies.usecases.queries.GetMovieRatingsQuery
import pt.ipbeja.moviesapi.entities.movies.usecases.queries.GetMovieRatingsQueryHandler
import pt.ipbeja.moviesapi.entities.movies.usecases.queries.GetMoviesQuery
import pt.ipbeja.moviesapi.entities.movies.usecases.queries.GetMoviesQueryHandler
import pt.ipbeja.moviesapi.utilities.*


fun Routing.movies() = route("/movies") {
    authenticate("basic") {
        get<MovieById> {

            val handler by inject<GetMovieQueryHandler>()

            when (val result = handler.handle(GetMovieQuery(it.id, call.principal<UserPrincipal>()?.id))) {
                is Success -> call.respond(result.value.toResponse())
                is Failure -> respondProblem(result)
            }
        }

        get<MoviePictureById> {

            val handler by inject<GetMoviePictureQueryHandler>()
            when (val result = handler.handle(GetMoviePictureQuery(it.id, it.pictureId))) {
                is Success<FileRepresentation> -> {
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Inline.withParameter(
                            ContentDisposition.Parameters.FileName,
                            result.value.filename
                        ).toString()
                    )

                    call.respondFile(result.value.file)
                }

                is Failure -> respondProblem(result)
            }


        }

        get<MoviesQuery> {

            val handler by inject<GetMoviesQueryHandler>()
            val results = handler.handle(
                GetMoviesQuery(
                    it.offset,
                    it.count,
                    it.director,
                    it.genre,
                    it.title,
                    it.fromReleaseDate,
                    it.toReleaseDate,
                    it.fromRating..it.toRating,
                    it.favoritesOnly && call.user.isUser,
                    it.sortBy,
                    it.sortOrder,
                    call.user.id
                )
            )

            call.respond(results.map { m ->
                MovieQueryResponse(
                    m.id,
                    m.title,
                    m.synopsis,
                    m.genres,
                    m.releaseDate,
                    m.director?.toResponse(),
                    m.mainPicture?.toResponse(),
                    m.rating,
                    m.favorite,
                    m.createdAt,
                    m.updatedAt
                )
            })

        }

        get<MovieRatings> {
            val handler by inject<GetMovieRatingsQueryHandler>()
            when (val result = handler.handle(GetMovieRatingsQuery(it.id))) {
                is Success -> call.respond(result.value.map { r -> r.toResponse() })
                is Failure -> respondProblem(result)
            }
        }

        post("/{id}/ratings") {
            val request = call.receive<SetUserRatingRequest>()
            if (!requireUser()) return@post

            val movieId = call.pathParameters["id"]?.toInt() ?: throw Exception()
            val user = call.principal<UserPrincipal>()!!

            val handler by inject<RateMovieCommandHandler>()

            handler.handle(RateMovieCommand(movieId, user.id, request.score.coerceIn(0..5), request.comment))

            call.respond(HttpStatusCode.NoContent)
        }

        delete("/{id}/ratings") {
            val movieId = call.pathParameters.getOrFail<Int>("id")
            val user = call.user
            val handler by inject<DeleteMovieRatingCommandHandler>()
            handler.handle(DeleteMovieRatingCommand(movieId, user.id))
            call.respond(HttpStatusCode.NoContent)
        }

        delete("/{id}/ratings/{userId}") {
            if(!requireAdmin()) return@delete
            val movieId = call.pathParameters.getOrFail<Int>("id")
            val user = call.pathParameters.getOrFail<Int>("userId")
            val handler by inject<DeleteMovieRatingCommandHandler>()
            handler.handle(DeleteMovieRatingCommand(movieId, user))
            call.respond(HttpStatusCode.NoContent)
        }

        put("/{id}/mark-as-favorite") {
            if (!requireUser()) return@put

            val movieId = call.pathParameters.getOrFail<Int>("id")
            val user = call.principal<UserPrincipal>()!!
            val isFavorite = call.queryParameters["value"]?.toBoolean() ?: true

            val handler by inject<SetUserFavoriteCommandHandler>()

            handler.handle(SetUserFavoriteCommand(movieId, user.id, isFavorite))
            call.respond(HttpStatusCode.NoContent)
        }


        post {
            if (!requireAdmin()) return@post

            val it = call.receive<CreateMovieRequest>()

            val command = CreateMovieCommand(
                it.title,
                it.synopsis,
                it.cast.map { c ->
                    RoleAssignment(c.personId, c.character)
                },
                it.pictures.map { p ->
                    CreatePicture(p.filename, p.data.decodeBase64Bytes(), p.description, false)
                },
                it.genres,
                it.directorId,
                it.releaseDate,
                it.minimumAge,)

            val handler by inject<CreateMovieCommandHandler>()

            val results = handler.handle(command)
            call.respond(results.toResponse() )
        }


        post("/multiple") {
            if (!requireAdmin()) return@post

            val request = call.receive<List<CreateMovieRequest>>()

            val command = CreateMoviesCommand(
                request.map {
                    CreateMovieCommand(
                        it.title,
                        it.synopsis,
                        it.cast.map { c ->
                            RoleAssignment(c.personId, c.character)
                        },
                        it.pictures.map { p ->
                            CreatePicture(p.filename, p.data.decodeBase64Bytes(), p.description, false)
                        },
                        it.genres,
                        it.directorId,
                        it.releaseDate,
                        it.minimumAge,
                    )
                }
            )
            val handler by inject<CreateMoviesCommandHandler>()

            val results = handler.handle(command)
            call.respond(results.map { it.toResponse() })
        }

        put<UpdateMovieRequest>("") {
            if (!requireAdmin()) return@put


            val command = UpdateMovieCommand(
                it.id,
                it.title,
                it.synopsis,
                it.genres.toSet(),
                it.directorId,
                it.releaseDate,
                it.minimumAge
            )


            val handler by inject<UpdateMovieCommandHandler>()
            val result = handler.handle(command)
            call.respond(result)
        }

        post("/{id}/cast") {

            val movieId = call.pathParameters["id"]!!.toInt()

            val assignment = call.receive<List<CastMemberAssignmentRequest>>()

            val castMembers = assignment.map { RoleAssignment(it.personId, it.character) }

            // val results = handler.handle(AddCastMembersCommand(movieId, castMembers))
            val handler by inject<AddCastMembersCommandHandler2>()
            val results2 = handler.handle(AddCastMembersCommand2(movieId, castMembers))


            call.respond(results2.map { it.toResponse() })
        }

        delete("/{id}/cast/{personId}") {
            val movieId = call.pathParameters["id"]!!.toInt()
            val personId = call.pathParameters["personId"]!!.toInt()
            val handler by inject<RemoveCastMembersCommandHandler>()

            handler.handle(RemoveCastMembersCommand(movieId, setOf(personId)))
            call.respond(HttpStatusCode.NoContent)
        }

        put("/{id}/cast/remove") {
            val membersIds = call.receive<Set<Int>>()
            val movieId = call.pathParameters["id"]!!.toInt()
            val handler by inject<RemoveCastMembersCommandHandler>()
            handler.handle(RemoveCastMembersCommand(movieId, membersIds))
            call.respond(HttpStatusCode.NoContent)
        }


        delete<MovieById> {
            if (!requireAdmin()) return@delete
            val handler by inject<DeleteMovieCommandHandler>()

            handler.handle(DeleteMovieCommand(it.id))
            call.respond(HttpStatusCode.NoContent)
        }


        delete<MoviePictureById> {
            if (!requireAdmin()) return@delete

            val handler by inject<RemoveMoviePicturesCommandHandler>()

            handler.handle(RemoveMoviePicturesCommand(it.id, setOf(it.pictureId)))
            call.respond(HttpStatusCode.NoContent)
        }


        put("/{id}/pictures/delete") {
            if (!requireAdmin()) return@put
            val movieId = call.pathParameters["id"]!!.toInt()
            val pictureIds = call.receive<Set<Int>>()

            val handler by inject<RemoveMoviePicturesCommandHandler>()

            handler.handle(RemoveMoviePicturesCommand(movieId, pictureIds))
            call.respond(HttpStatusCode.NoContent)
        }

    }


}

@Resource("")
class MoviesQuery(
    val offset: Long = 0,
    val count: Int = Int.MAX_VALUE,
    val director: Int? = null,
    val genre: String? = null,
    val title: String? = null,
    val fromReleaseDate: LocalDate? = null,
    val toReleaseDate: LocalDate? = null,
    val fromRating: Int = 0,
    val toRating: Int = 5,
    val favoritesOnly: Boolean = false,
    val sortBy: String = "releaseDate",
    val sortOrder: String = "desc"
)


@Resource("/{id}")
class MovieById(val id: Int)


@Resource("/{id}/mark-as-favorite")
class MarkAsFavorite(val id: Int)


@Resource("/{id}/pictures/{pictureId}")
class MoviePictureById(val id: Int, val pictureId: Int)


@Resource("/{id}/ratings")
class MovieRatings(val id: Int)


@Serializable
class SetUserRatingRequest(val score: Int, val comment: String?)