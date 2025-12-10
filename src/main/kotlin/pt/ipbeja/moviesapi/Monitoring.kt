package pt.ipbeja.moviesapi


import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*

fun Application.configureMonitoring() {
    // install(KHealth)

    install(CallId) {
        header(HttpHeaders.XRequestId)
        verify { callId: String ->
            callId.isNotEmpty()
        }
    }
    install(CallLogging) {
        callIdMdc("call-id")
    }
}
