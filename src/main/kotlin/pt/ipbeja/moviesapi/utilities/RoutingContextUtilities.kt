package pt.ipbeja.moviesapi.utilities

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import pt.ipbeja.moviesapi.entities.common.JsonExtensions
import pt.ipbeja.moviesapi.entities.common.ProblemDetails

suspend fun <T> RoutingContext.okOrNotFound(data: T?, toDto: (T.() -> Any)? = null) {
    if (data != null) {
        call.respond(HttpStatusCode.OK, toDto?.let { it(data) } ?: data)
    } else {
        call.respond(HttpStatusCode.NotFound)
    }
}

suspend fun RoutingContext.noContentOrNotFound(data: Any?) {
    if (data != null) {
        call.respond(HttpStatusCode.NoContent)
    } else {
        call.respond(HttpStatusCode.NotFound)
    }
}

suspend fun RoutingContext.respond(data: Any?, success: HttpStatusCode, ifNull: HttpStatusCode) {
    if (data != null) call.respond(success, data) else call.respond(ifNull)
}


suspend fun RoutingContext.respond(
    data: Any?,
    success: HttpStatusCode = HttpStatusCode.OK,
    ifNull: () -> ProblemDetails = {
        ProblemDetails(
            "NotFound",
            "Resource not found",
            404
        )
    }
) {
    if (data != null) call.respond(success, data)
    else {
        val pd = ifNull()

        call.response.header(HttpHeaders.ContentType, ContentType.Application.ProblemJson.toString())
        call.respond(HttpStatusCode.fromValue(pd.status), pd)
    }
}

suspend fun RoutingContext.respondProblem(failure: Failure) {
    val problem = if (failure.errors.all { it.errorType == ErrorTypes.validation }) {
        ProblemDetails(
            "Validation",
            "Validation failed",
            400,
            extensions = JsonExtensions(mapOf("errors" to failure.errors.map { it.message }))
        )

    } else {

        val error = failure.errors.first()
        ProblemDetails(
            error.code,
            error.code,
            error.errorType,
            error.message
        )
    }

    call.response.header(HttpHeaders.ContentType, ContentType.Application.ProblemJson.toString())
    call.respond(HttpStatusCode.fromValue(problem.status), problem)
}