package pt.ipbeja.moviesapi.entities.people


import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import org.koin.ktor.ext.inject
import pt.ipbeja.moviesapi.entities.common.CreatePictureRequest
import pt.ipbeja.moviesapi.entities.common.toCreateDefinition
import pt.ipbeja.moviesapi.entities.common.toResponse
import pt.ipbeja.moviesapi.entities.people.usecases.commands.*
import pt.ipbeja.moviesapi.entities.people.usecases.queries.GetPeopleQuery
import pt.ipbeja.moviesapi.entities.people.usecases.queries.GetPeopleQueryHandler
import pt.ipbeja.moviesapi.entities.people.usecases.queries.GetPersonPictureQuery
import pt.ipbeja.moviesapi.entities.people.usecases.queries.GetPersonPictureQueryHandler
import pt.ipbeja.moviesapi.entities.people.usecases.queries.GetPersonQuery
import pt.ipbeja.moviesapi.entities.people.usecases.queries.GetPersonQueryHandler
import pt.ipbeja.moviesapi.utilities.Failure
import pt.ipbeja.moviesapi.utilities.Success
import pt.ipbeja.moviesapi.utilities.requireAdmin
import pt.ipbeja.moviesapi.utilities.respondProblem

/**
 * @author Diogo Pina Manique
 * @version 02/12/2025
 */

fun Routing.people() = route("/people") {

    get {
        val handler by inject<GetPeopleQueryHandler>()
        val results = handler.handle(GetPeopleQuery)

        call.respond(results.map { PersonResponse(it.id, it.name, it.dateOfBirth, it.picture?.toResponse()) })
    }

    get("/{id}") {

        val id = call.pathParameters.getOrFail<Int>("id")

        val handler by inject<GetPersonQueryHandler>()
        when (val result = handler.handle(GetPersonQuery(id))) {
            is Success -> call.respond(result.value.toResponse())
            is Failure -> respondProblem(result)
        }
    }


    get("/{id}/picture/{picId}") {

        val id = call.pathParameters.getOrFail<Int>("id")
        val picId = call.pathParameters.getOrFail<Int>("picId")

        val handler by inject<GetPersonPictureQueryHandler>()
        val result = handler.handle(GetPersonPictureQuery(id, picId))

        call.respondFile(result.file)
    }


    authenticate("basic") {

        post<CreatePersonRequest> {
            if (!requireAdmin()) return@post

            val person = it.toCommand()
            val handler by inject<CreatePeopleCommandHandler>()
            val result = handler.handle(CreatePeopleCommand(listOf(person)))
            call.respond(result.map { p -> p.toResponse() }.first())
        }

        post<List<CreatePersonRequest>>("/multiple") {
            if (!requireAdmin()) return@post

            val people = it.map { p -> p.toCommand() }
            val handler by inject<CreatePeopleCommandHandler>()
            val result = handler.handle(CreatePeopleCommand(people))
            call.respond(result.map { p -> p.toResponse() })
        }

        put<UpdatePersonRequest> {
            if (!requireAdmin()) return@put

            val handler by inject<UpdatePersonCommandHandler>()
            val result = handler.handle(UpdatePersonCommand(it.id, it.name, it.dateOfBirth))
            call.respond(result.toResponse())
        }


        put<List<CreatePictureRequest>>("/{id}/add-pictures") {
            if (!requireAdmin()) return@put

            val id = call.pathParameters.getOrFail<Int>("id")
            val handler by inject<AddPicturesToPersonCommandHandler>()
            val result = handler.handle(AddPicturesToPersonCommand(id, it.map { p -> p.toCreateDefinition() }))
            call.respond(result.toResponse())
        }


        put<Set<Int>>("/{id}/remove-pictures") {
            if (!requireAdmin()) return@put
            val id = call.pathParameters.getOrFail<Int>("id")

            val handler by inject<RemovePicturesFromPersonCommandHandler>()
            val result = handler.handle(RemovePicturesFromPersonCommand(id, it))
            call.respond(result.toResponse())
        }



        delete("/{id}") {
            if (!requireAdmin()) return@delete

            val personId = call.pathParameters.getOrFail<Int>("id")


            val handler by inject<DeletePeopleCommandHandler>()
            val result = handler.handle(DeletePeopleCommand(setOf(personId)))
            call.respond(HttpStatusCode.NoContent)

        }

        put<Set<Int>>("/delete") {
            if (!requireAdmin()) return@put

            val handler by inject<DeletePeopleCommandHandler>()
            val result = handler.handle(DeletePeopleCommand(it))
            call.respond(HttpStatusCode.NoContent)
        }


    }


}
