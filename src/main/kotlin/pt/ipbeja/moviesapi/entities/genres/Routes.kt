package pt.ipbeja.moviesapi.entities.genres


import io.ktor.http.*
import io.ktor.resources.Resource
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.get
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.getKoin
import org.koin.ktor.ext.inject
import pt.ipbeja.moviesapi.entities.genres.model.Genre
import pt.ipbeja.moviesapi.entities.genres.usecases.commands.CreateGenre
import pt.ipbeja.moviesapi.entities.genres.usecases.commands.CreateGenresCommand
import pt.ipbeja.moviesapi.entities.genres.usecases.commands.CreateGenresCommandHandler
import pt.ipbeja.moviesapi.entities.genres.usecases.commands.DeleteGenresCommand
import pt.ipbeja.moviesapi.entities.genres.usecases.commands.DeleteGenresCommandHandler
import pt.ipbeja.moviesapi.entities.genres.usecases.commands.UpdateGenreCommand
import pt.ipbeja.moviesapi.entities.genres.usecases.commands.UpdateGenreCommandHandler
import pt.ipbeja.moviesapi.entities.genres.usecases.queries.GetGenresQuery
import pt.ipbeja.moviesapi.entities.genres.usecases.queries.GetGenresQueryHandler
import pt.ipbeja.moviesapi.utilities.Failure
import pt.ipbeja.moviesapi.utilities.Success
import pt.ipbeja.moviesapi.utilities.requireAdmin
import pt.ipbeja.moviesapi.utilities.respondProblem
import pt.ipbeja.moviesapi.utilities.send

fun Genre.toResponse() = GenreResponse(id, name, description)


@Resource("")
data class GetGenres(val assignedOnly: Boolean = false)


fun Route.genres() = route("/genres") {


    get<GetGenres> {
        val handler by inject<GetGenresQueryHandler>()
        val results = handler.handle(GetGenresQuery(it.assignedOnly))
        val response = results.map { it.toResponse() }
        call.respond(response)
    }

    authenticate("basic") {



        post {
            if(!requireAdmin()) return@post
            val request = call.receive<List<CreateGenreRequest>>().map { CreateGenre(it.name, it.description) }


            val handler by inject<CreateGenresCommandHandler>()
            when (val results = handler.handle(CreateGenresCommand(request))) {
                is Failure -> respondProblem(results)
                is Success -> call.respond(results.value.map { it.toResponse() })
            }

        }

        put {
            if(!requireAdmin()) return@put

            val (id, name, description) = call.receive<UpdateGenreRequest>()

            val handler by inject<UpdateGenreCommandHandler>()
            when (val result = handler.handle(UpdateGenreCommand(id, name, description))) {
                is Success -> call.respond(result.value.toResponse())
                is Failure -> respondProblem(result)
            }
        }

        delete("/{id}") {
            if(!requireAdmin()) return@delete

            val id = call.pathParameters["id"]!!.toInt()
            val handler by inject<DeleteGenresCommandHandler>()

            handler.handle(DeleteGenresCommand(setOf(id)))
            call.respond(HttpStatusCode.NoContent)
        }

        put("/delete") {
            if(!requireAdmin()) return@put

            val ids = call.receive<Set<Int>>()

            val handler by inject<DeleteGenresCommandHandler>()
            handler.handle(DeleteGenresCommand(ids))
            call.respond(HttpStatusCode.NoContent)
        }
    }

}