package pt.ipbeja.moviesapi

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.jdbc.Database
import pt.ipbeja.moviesapi.entities.genres.genres
import pt.ipbeja.moviesapi.entities.movies.movies
import pt.ipbeja.moviesapi.entities.people.people
import pt.ipbeja.moviesapi.entities.users.users


fun Application.configureRouting(database: Database) {
    routing {
        // OpenAPI/Swagger Documentation
        swaggerUI(path = "openapi", swaggerFile = "openapi/documentation.yaml")
        swaggerUI(path = "openapi/movies", swaggerFile = "openapi/movies.yaml")
        swaggerUI(path = "openapi/genres", swaggerFile = "openapi/genres.yaml")
        swaggerUI(path = "openapi/people", swaggerFile = "openapi/people.yaml")
        swaggerUI(path = "openapi/users", swaggerFile = "openapi/users.yaml")
        swaggerUI(path = "openapi/system", swaggerFile = "openapi/system.yaml")

        // API Test Interface - no authentication required
        get("/test") {
            val html = this::class.java.classLoader
                .getResourceAsStream("static/test/index.html")
                ?.bufferedReader()
                ?.readText()
                ?: return@get call.respondText("Test page not found", status = HttpStatusCode.NotFound)
            call.respondText(html, ContentType.Text.Html)
        }
    }

    routing {
        configureFrameworks(database)

        // Chaos engineering endpoint for testing - disable in production
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

        // API Routes
        movies()
        users()
        genres()
        people()
    }
}
