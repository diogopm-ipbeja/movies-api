package pt.ipbeja.moviesapi

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
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
        swaggerUI(path = "swagger", swaggerFile = "./openapi/doc.yaml")
        // swaggerUI(path = "openapi", swaggerFile = "/app/openapi/documentation.yaml")
        swaggerUI(path = "swagger/movies", swaggerFile = "./openapi/movies.yaml")
        swaggerUI(path = "swagger/genres", swaggerFile = "./openapi/genres.yaml")
        swaggerUI(path = "swagger/people", swaggerFile = "./openapi/people.yaml")
        swaggerUI(path = "swagger/users", swaggerFile =  "./openapi/users.yaml")
        swaggerUI(path = "swagger/system", swaggerFile = "./openapi/system.yaml")

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


        movies()
        users()
        genres()
        people()

    }

}
