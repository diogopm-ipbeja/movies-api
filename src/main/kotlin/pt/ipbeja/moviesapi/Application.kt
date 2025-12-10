package pt.ipbeja.moviesapi

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.resources.Resources
import io.ktor.server.response.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.serialization.appJson
import pt.ipbeja.moviesapi.utilities.UserPrincipal
import pt.ipbeja.moviesapi.utilities.hash
import pt.ipbeja.moviesapi.utilities.verify
import kotlin.random.Random

fun main(args: Array<String>) {
    EngineMain.main(args)
}

@Serializable
object SimulateFailuresConfig {
    var failureRate: Float = 0f
    var minimumDelay: Int = 0
    var maximumDelay: Int = 0
}

val SimulateFailuresPlugin = createApplicationPlugin(
    name = "SimulateFailures",
    createConfiguration = { SimulateFailuresConfig }
) {
    val failureRate = pluginConfig.failureRate

    onCallReceive { call ->

        val minDelay = pluginConfig.minimumDelay
        val maxDelay = pluginConfig.maximumDelay
        if(minDelay > 0 || maxDelay > 0) {
            val delayMs = Random.nextInt(minDelay, maxDelay).toLong()
            delay(delayMs)
        }
        if(failureRate > 0f && Random.nextFloat() < failureRate) {
            call.respond(HttpStatusCode.InternalServerError, "Simulated failure")
        }
    }

}



fun Application.module() {

    install(SimulateFailuresPlugin) {
        this.failureRate = 0f
        this.maximumDelay = 0
        this.minimumDelay = 0
    }

    install(ContentNegotiation) {
        json(appJson)
    }

    val dbUser = environment.config.property("database.user").getString()
    val dbPassword = environment.config.property("database.password").getString()
    val dbHost = environment.config.property("database.host").getString()
    val dbPort = environment.config.property("database.port").getAs<Int>()
    val dbDatabase = environment.config.property("database.db").getString()

    val schema = Schema("movies-api")
    val database = Database.connect(
        "jdbc:postgresql://$dbHost:$dbPort/$dbDatabase",
        driver = "org.postgresql.Driver",
        user = dbUser,
        password = dbPassword,
        databaseConfig = DatabaseConfig {
            defaultSchema = schema
        }
    )


    val adminUser = environment.config.property("administration.user").getString()
    val adminPassword = environment.config.property("administration.password").getString()

    transaction {
        SchemaUtils.createSchema(schema)
        SchemaUtils.create(
            Movies,
            Genres,
            MovieGenres,
            MovieRatings,
            Users,
            CastMembers,
            MoviePictures,
            Favorites,
            Persons,
            PersonPictures
        )


        val adminUsersCount = UserEntity.count(Users.role eq "admin")
        if (adminUsersCount == 0L) {
            UserEntity.new {
                username = adminUser
                passwordHash = adminPassword.hash()
                role = "admin"
            }

            println("Created admin user '$adminUser'")
        }

        val normalUserCount = UserEntity.count(Users.role eq "user")

        if (normalUserCount == 0L) {
            val times = 4
            repeat(times) {
                UserEntity.new {
                    username = "user${it + 1}"
                    passwordHash = "user${it + 1}".hash()
                    role = "user"
                }
            }
            val lastIndex = times + 1
            println("Created $times users (user1/user1 .. user$lastIndex/user$lastIndex)")
        }


    }




    install(Authentication) {
        basic("basic") {

            validate {
                val user = if (it.name == adminUser) {
                    if (it.password == adminPassword) UserPrincipal(0, adminUser, "admin")
                    else null
                } else {
                    transaction(database) {
                        UserEntity.find { Users.username eq it.name }.firstOrNull()?.let { dbUser ->
                            val verified = it.password.verify(dbUser.passwordHash).verified
                            if (verified) UserPrincipal(dbUser.id.value, dbUser.username, dbUser.role) else null
                        }
                    }
                }
                if (user == null) respond(HttpStatusCode.Unauthorized)
                user
            }

        }
    }

    // pt.ipbeja.moviesapi.configureFrameworks()
    // configureMonitoring()
    // configureHTTP()

    install(Resources)
    configureRouting(database)
}
