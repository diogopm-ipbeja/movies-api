package pt.ipbeja.moviesapi

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.jdbc.Database
import pt.ipbeja.moviesapi.entities.genres.genres
import pt.ipbeja.moviesapi.entities.movies.movies
import pt.ipbeja.moviesapi.entities.people.people
import pt.ipbeja.moviesapi.entities.users.users


@Resource("/")
data class MovieQueryLocation(
    val offset: Long = 0,
    val count: Int = Int.MAX_VALUE,
    val director: Int? = null,
    val genre: String? = null,
    val title: String? = null,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val fromRating: Int? = null,
    val toRating: Int? = null,
    val favoritesOnly: Boolean = false,
    val sortBy: String = "releaseDate",
    val sortOrder: String = "desc"
)

/*class AuthorizationPluginConfiguration {
    var roles: Set<String> = emptySet()
}

// Custom role-based plugin
val RoleBasedAuthorizationPlugin = createRouteScopedPlugin(
    name = "RBAC-Plugin",
    createConfiguration = ::AuthorizationPluginConfiguration
) {
    val allowedRoles = pluginConfig.roles
    pluginConfig.apply {
        on(AuthenticationChecked) { call ->
            val role = call.principal<UserPrincipal>()?.role
            if (role !in allowedRoles) {
                call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}

fun Route.authorized(
    vararg allowedRoles: String,
    build: Route.() -> Unit
) {
    install(RoleBasedAuthorizationPlugin) {
        roles = allowedRoles.toSet()
    }
    build()
}

val RoutingContext.isAdmin: Boolean
    get() = this.call.principal<UserPrincipal>()?.role == "admin"*/


fun Application.configureRouting(database: Database) {
    routing {
        swaggerUI(path = "openapi"/*, swaggerFile = "openapi/documentation.json"*/)
    }

    /*install(SSE)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val problemDetails = ProblemDetails(
                title = "Internal Server Error",
                status = HttpStatusCode.InternalServerError.value,
                detail = "An unexpected error occurred. Contact the service administrator if this error persists.",
                instance = call.request.path()
            )
            call.respond(HttpStatusCode.InternalServerError, problemDetails)
        }
    }

    install(RequestValidation) {
        validate<String> { bodyText ->
            if (!bodyText.startsWith("Hello"))
                ValidationResult.Invalid("Body text should start with 'Hello'")
            else ValidationResult.Valid
        }
    }
    install(DoubleReceive)*/


    routing {
        configureFrameworks(database)
        route("failures") {

            get {
                call.respond(SimulateFailuresConfig)
            }

            @Serializable
            data class FailureConfig(val minimumDelay: Int, val maximumDelay: Int, val failureRate: Float)
            put<FailureConfig> {
                SimulateFailuresConfig.minimumDelay = it.minimumDelay
                SimulateFailuresConfig.maximumDelay = it.maximumDelay
                SimulateFailuresConfig.failureRate = it.failureRate
                call.respond(it)
            }

        }

        // openApi("/opt/api.json")

        movies()
        users()
        genres()
        people()

        /*authenticate("basic") {
            route("/users") {
                post {
                    val (username, password, role) = call.receive<RegisterUserRequest>()

                    RegisterUserUseCase(database).handle(username, password, role)
                }

                put("/set-picture") {

                    *//* val userId = 1
                     val part = call.receiveMultipart().readPart() as? PartData.FileItem ?: throw Exception()
                     val bytes = part.provider().toByteArray()

                     val user = UserEntity[userId]

                     val tx = StorageService(Path(".")).createTransaction("users")
                     user.picture?.let {
                         tx.removeFile(it)
                     }*//*
                }

                delete("/remove-picture") {


                }

                put {


                }


                put("/change-password") {
                    val principal = call.principal<Int>() ?: throw Exception()
                    val (currentPassword, newPassword) = call.receive<ChangeUserPasswordRequest>()

                    val result = transaction(database) {
                        val user = UserEntity[principal]

                        val oldPassHash = currentPassword.pt.ipbeja.hash()
                        if (oldPassHash != user.passwordHash) {
                            return@transaction HttpStatusCode.BadRequest to "Password does not match"
                        }

                        val newPassHash = newPassword.pt.ipbeja.hash()
                        if (newPassHash == user.passwordHash) {
                            return@transaction HttpStatusCode.BadRequest to "New password is the same as the current password."
                        }

                        user.passwordHash = newPassHash
                        HttpStatusCode.NoContent to ""
                    }

                    call.respond(result.first, result.second)
                }
            }


            route("/people") {
                post {
                    val request = call.receive<CreatePersonRequest>()

                    val handler = CreatePersonCommandHandler(database, StorageService(Path.of("/")))
                    val person = handler.handle(CreatePersonCommand(
                        request.name, request.dateOfBirth, request.pictures.map { CreatePicture(it.filename, it.data.decodeBase64Bytes(), it.description, it.defaultPicture) }
                    ))
                    *//*val person = CreatePersonUseCase(database, StorageService(Path.of("/"))).handle(
                        request.name,
                        request.dateOfBirth,
                        emptyList()
                    )*//*

                    call.respond(PersonResponse(person.id, person.name, person.dateOfBirth))
                }
                get("/{id}") {

                    val id = call.pathParameters["id"]?.toInt() ?: throw Exception()

                    val person = GetPersonUseCase(database).handle(id)
                    call.respond(person.toResponse())
                }
            }

            *//*route("/genres") {
                post {

                    val genres = call.receive<List<CreateGenreRequest>>()

                    val createGenres = genres.map {
                        CreateGenresCommand(
                            it.name,
                            it.description
                        )
                    }

                    val results = CreateGenresCommandHandler(database)
                        .handle(createGenres.toSet())

                    call.respond(results.map { GenreResponse(it.id, it.name, it.description) })

                }

                get {

                    val genres = transaction { Genres.selectAll().map { it.toGenre() } }

                    val map = genres.map { GenreResponse(it.id, it.name, it.description) }
                    call.respond(map)
                }
            }*//*
            route("/movies") {

                get<MovieQueryLocation> {

                    val userId = call.principal<UserPrincipal>()?.let { p ->
                        if(p.role == "admin") null else p.id
                    }

                    val fromRating = it.fromRating ?: 0
                    val toRating = it.toRating ?: 5

                    val results = GetMoviesQueryHandler(database).handle(
                        GetMoviesQuery(
                            it.offset,
                            it.count,
                            it.director,
                            it.genre,
                            it.title,
                            it.fromDate,
                            it.toDate,
                            fromRating..toRating,
                            it.favoritesOnly && userId != null,
                            it.sortBy,
                            it.sortOrder,
                            userId
                        ),
                    )

                    call.respond(results.map { m ->
                        pt.ipbeja.contracts.MovieQueryResponse(
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

                // authorizedRoute("admin") {
*//*
                    post {

                        if (!this.isAdmin()) return@post

                        val storageService = StorageService(".")
                        val movie = call.receive<CreateMovieRequest>()


                        val pics = movie.pictures.map {

                            CreatePicture(it.filename, it.data.decodeBase64Bytes()*//*
*//*Base64.decode(it.data)*//**//*
, it.description, it.defaultPicture)
                        }

                        val newMovie = CreateMovieCommandHandler(database, storageService).handle(
                            CreateMovieCommand(
                                movie.title,
                                movie.synopsis,
                                movie.cast.map { CreateMovieCommand.CastMember(it.personId, it.character) },
                                pics,
                                movie.genres,
                                movie.directorId,
                                movie.releaseDate
                            )
                        )

                        call.respond(
                            MovieDetailResponse(
                                newMovie.id,
                                newMovie.title,
                                newMovie.synopsis,
                                newMovie.genres.map { GenreResponse(it.id, it.name, it.description) }.toSet(),
                                newMovie.director?.run {
                                    DirectorResponse(
                                        personId,
                                        name,
                                        picture?.let { p ->
                                            PictureInfoResponse(
                                                p.id,
                                                p.mainPicture,
                                                p.filename,
                                                p.contentType,
                                                p.description
                                            )
                                        }
                                    )
                                },
                                newMovie.releaseDate,
                                null,
                                newMovie.pictures.map {
                                    PictureInfoResponse(
                                        it.id,
                                        it.mainPicture,
                                        it.filename,
                                        it.contentType,
                                        it.description
                                    )
                                },
                                newMovie.createdAt,
                                null,
                                newMovie.cast.map { it.toResponse() }
                            )
                        )
                    }
*//*

                // }

                get("/{id}") {
                    val id = call.pathParameters["id"]?.toInt() ?: throw Exception()

                    val newMovie = GetMovieQueryHandler(database).handle(id)

                    call.respond(
                        MovieDetailResponse(
                            newMovie.id,
                            newMovie.title,
                            newMovie.synopsis,
                            newMovie.genres.map { GenreResponse(it.id, it.name, it.description) }.toSet(),
                            newMovie.director?.run {
                                DirectorResponse(
                                    personId,
                                    name,
                                    picture?.let { p ->
                                        PictureInfoResponse(
                                            p.id,
                                            p.mainPicture,
                                            p.filename,
                                            p.contentType,
                                            p.description
                                        )
                                    }
                                )
                            }
                            ,
                            newMovie.releaseDate,
                            newMovie.rating,

                            newMovie.pictures.map {
                                PictureInfoResponse(
                                    it.id,
                                    it.mainPicture,
                                    it.filename,
                                    it.contentType,
                                    it.description
                                )
                            },
                            newMovie.createdAt,
                            newMovie.updatedAt,
                            newMovie.cast.map { it.toResponse() }
                        )
                    )
                }


                post("/{id}/rate") {
                    if(!isUser()) return@post

                    transaction {

                        val id = call.pathParameters["id"]?.toInt() ?: throw Exception()

                        val movie = MovieEntity[id]
                        val user = UserEntity[1]

                        MovieRatingEntity.new {
                            this.movie = movie
                            this.user = user
                            this.score = 3
                            this.comment = "kinda sucks"
                        }

                    }

                }

            }

        }*/


        /*route("/movies") {
            // authenticate("basic") {

            // authorized("admin") {
            createMovie()
            deleteMovie()
            deleteMovies()
            // }

            getMovieById()
            getMovies()
            // }

        }*/
    }

}
