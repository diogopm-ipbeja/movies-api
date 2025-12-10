package pt.ipbeja.moviesapi.utilities

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


data class UserPrincipal(val id: Int, val username: String, val role: String)

class AuthorizationPluginConfiguration {
    var roles: Set<String> = emptySet()
}

val RoleBasedAuthorizationPlugin = createRouteScopedPlugin(
    name = "RBAC-Plugin",
    createConfiguration = ::AuthorizationPluginConfiguration
) {
    val allowedRoles = pluginConfig.roles
    pluginConfig.apply {
        on(AuthenticationChecked) { call ->
            val principal = call.principal<UserPrincipal>()
            if (principal == null) {
                call.respond(HttpStatusCode.Unauthorized, "Authentication required")
                return@on
            }
            if (principal.role !in allowedRoles) {
                call.respond(HttpStatusCode.Forbidden, "'${principal.username}' is not authorized for this operation")
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

val RoutingCall.user : UserPrincipal
    get() = principal<UserPrincipal>()
        ?: throw IllegalStateException("User principal not found. Ensure this is called within an authenticated route.")


val UserPrincipal.isUser : Boolean
    get() = role == "user"

val UserPrincipal.isAdmin : Boolean
    get() = role == "admin"

fun UserPrincipal.isAdminOrOwner(userId: Int) = isAdmin || id == userId


suspend fun RoutingContext.requireAdmin(): Boolean {
    val principal = call.principal<UserPrincipal>()
    if(principal == null) {
        call.respond(HttpStatusCode.Unauthorized)
        return false
    }
    val hasRole = principal.role == "admin"
    if(!hasRole) call.respond(HttpStatusCode.Forbidden, "'${principal.username}' is not an administrator")
    return hasRole
}

suspend fun RoutingContext.requireUser(): Boolean {
    val principal = call.principal<UserPrincipal>()
    if(principal == null) {
        call.respond(HttpStatusCode.Unauthorized)
        return false
    }
    val isUser = principal.role == "user"
    if(!isUser) call.respond(HttpStatusCode.Forbidden, "'${principal.username}' is not normal user")
    return isUser
}



fun Route.authorizedRoute(
    vararg allowedRoles: String,
    build: Route.() -> Unit
) {
    route("") {
        install(RoleBasedAuthorizationPlugin) {
            roles = allowedRoles.toSet()
        }
        build()
    }

}

fun Route.authorizedRoute(
    path: Regex,
    vararg allowedRoles: String,
    build: Route.() -> Unit
) {
    install(RoleBasedAuthorizationPlugin) {
        roles = allowedRoles.toSet()
    }
    route(path) {
        build()
    }

}


