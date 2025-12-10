package pt.ipbeja.moviesapi.entities.users

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import org.koin.ktor.ext.inject
import pt.ipbeja.moviesapi.entities.common.CreatePictureRequest
import pt.ipbeja.moviesapi.entities.common.toCreateDefinition
import pt.ipbeja.moviesapi.entities.users.usecases.commands.DeleteUserCommand
import pt.ipbeja.moviesapi.entities.users.usecases.commands.DeleteUserCommandHandler
import pt.ipbeja.moviesapi.entities.users.usecases.commands.RegisterUserCommand
import pt.ipbeja.moviesapi.entities.users.usecases.commands.RegisterUserCommandHandler
import pt.ipbeja.moviesapi.entities.users.usecases.commands.SetUserPictureCommand
import pt.ipbeja.moviesapi.entities.users.usecases.commands.SetUserPictureCommandHandler
import pt.ipbeja.moviesapi.entities.users.usecases.queries.GetUserPictureQuery
import pt.ipbeja.moviesapi.entities.users.usecases.queries.GetUserPictureQueryHandler
import pt.ipbeja.moviesapi.entities.users.usecases.queries.GetUserQuery
import pt.ipbeja.moviesapi.entities.users.usecases.queries.GetUserQueryHandler
import pt.ipbeja.moviesapi.entities.users.usecases.queries.GetUsersQuery
import pt.ipbeja.moviesapi.entities.users.usecases.queries.GetUsersQueryHandler
import pt.ipbeja.moviesapi.utilities.Failure
import pt.ipbeja.moviesapi.utilities.Success
import pt.ipbeja.moviesapi.utilities.requireAdmin
import pt.ipbeja.moviesapi.utilities.respondProblem
import pt.ipbeja.moviesapi.utilities.user


fun Routing.users() = route("/users") {


    post<RegisterUserRequest>("/register") {
        val handler by inject<RegisterUserCommandHandler>()
        val result = handler.handle(RegisterUserCommand(it.username, it.password, "user", it.dateOfBirth, it.picture?.toCreateDefinition()))

        when (result) {
            is Success -> call.respond(PrivateUserResponse(result.value.id, result.value.username, result.value.dateOfBirth, result.value.createdAt, null))
            is Failure -> respondProblem(result)
        }
    }


    authenticate("basic") {

        post<RegisterUserRequest>("/register/admin") {

            if(call.user.username != environment.config.property("administration.user").getString()) {
                call.respond(HttpStatusCode.Forbidden, "'${call.user.username}' is not the master administrator")
                return@post
            }

            val handler by inject<RegisterUserCommandHandler>()
            val result = handler.handle(
                RegisterUserCommand(
                    it.username,
                    it.password,
                    "admin",
                    it.dateOfBirth,
                    it.picture?.toCreateDefinition()
                )
            )
            when (result) {
                is Success -> call.respond(PrivateUserResponse(result.value.id, result.value.username, result.value.dateOfBirth, result.value.createdAt, null))
                is Failure -> respondProblem(result)
            }
        }

        get("/login") {
            val user = call.user
            call.respond(HttpStatusCode.OK, LoginResponse(user.id, user.username, user.role))
        }


        get {
            if (!requireAdmin()) return@get
            val handler by inject<GetUsersQueryHandler>()
            val results = handler.handle(GetUsersQuery)
            call.respond(results.map {
                PrivateUserResponse(
                    it.id,
                    it.username,
                    it.dateOfBirth,
                    it.createdAt,
                    it.updatedAt
                )
            })
        }

        get("/{id}") {

            val id = call.pathParameters.getOrFail<Int>("id")
            val handler by inject<GetUserQueryHandler>()
            when (val result = handler.handle(GetUserQuery(id))) {
                is Success -> call.respond(UserResponse(result.value.id, result.value.username, result.value.createdAt, result.value.updatedAt))
                is Failure -> respondProblem(result)
            }

        }

        get("/{id}/detail") {
            if(!requireAdmin()) return@get
            val id = call.pathParameters.getOrFail<Int>("id")
            val handler by inject<GetUserQueryHandler>()
            when (val result = handler.handle(GetUserQuery(id))) {
                is Success -> call.respond(PrivateUserResponse(result.value.id, result.value.username, result.value.dateOfBirth,result.value.createdAt, result.value.updatedAt))
                is Failure -> respondProblem(result)
            }

        }

        get("/self") {


            val handler by inject<GetUserQueryHandler>()
            when (val result = handler.handle(GetUserQuery(call.user.id))) {
                is Success -> call.respond(PrivateUserResponse(result.value.id, result.value.username, result.value.dateOfBirth,result.value.createdAt, result.value.updatedAt))
                is Failure -> respondProblem(result)
            }
        }


        get("/{id}/picture") {

            val id = call.pathParameters.getOrFail<Int>("id")
            val handler by inject<GetUserPictureQueryHandler>()
            when (val result = handler.handle(GetUserPictureQuery(id))) {
                is Success -> call.respondFile(result.value.file)
                is Failure -> respondProblem(result)
            }

        }

        put<CreatePictureRequest>("/{id}/picture") {
            if(!requireAdmin()) return@put
            val id = call.pathParameters.getOrFail<Int>("id")
            val handler by inject<SetUserPictureCommandHandler>()
            val user = handler.handle(SetUserPictureCommand(id, it.toCreateDefinition()))
            call.respond(HttpStatusCode.NoContent)
        }

        put<CreatePictureRequest>("/self/picture") {
            val handler by inject<SetUserPictureCommandHandler>()
            val user = handler.handle(SetUserPictureCommand(call.user.id, it.toCreateDefinition()))
            call.respond(HttpStatusCode.NoContent)
        }


        delete("/{id}/picture") {
            if(!requireAdmin()) return@delete
            val id = call.pathParameters.getOrFail<Int>("id")
            val handler by inject<SetUserPictureCommandHandler>()
            val user = handler.handle(SetUserPictureCommand(id, null))
            call.respond(HttpStatusCode.NoContent)
        }

        delete("/self/picture") {
            val handler by inject<SetUserPictureCommandHandler>()
            val user = handler.handle(SetUserPictureCommand(call.user.id, null))
            call.respond(HttpStatusCode.NoContent)
        }




        delete("/{id}") {
            val currentUser = call.user
            val userId = call.pathParameters.getOrFail<Int>("id")

            // Check if trying to delete master admin
            if (environment.config.property("administration.user").getString() == currentUser.username && currentUser.id == userId) {
                call.respond(HttpStatusCode.Forbidden, "Cannot delete the master administrator")
                return@delete
            }

            // Users can only delete themselves, admins can delete anyone
            if (currentUser.role != "admin" && currentUser.id != userId) {
                call.respond(HttpStatusCode.Forbidden, "You can only delete your own account")
                return@delete
            }

            val handler by inject<DeleteUserCommandHandler>()
            handler.handle(DeleteUserCommand(userId))
            call.respond(HttpStatusCode.NoContent)
        }

        delete("/self") {
            val currentUser = call.user

            // Cannot delete master admin
            if (environment.config.property("administration.user").getString() == currentUser.username) {
                call.respond(HttpStatusCode.Forbidden, "Cannot delete the master administrator")
                return@delete
            }

            val handler by inject<DeleteUserCommandHandler>()
            handler.handle(DeleteUserCommand(currentUser.id))
            call.respond(HttpStatusCode.NoContent)
        }

    }


}