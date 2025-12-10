package pt.ipbeja.moviesapi.utilities

import kotlinx.datetime.LocalDate
import org.koin.java.KoinJavaComponent.inject
import org.koin.java.KoinJavaComponent.injectOrNull


fun interface Validator<T> {
    fun validate(value: T)
}


val firstMovieEver = LocalDate(1888, 10, 14)
const val maxFileSize = 1024 * 1024



suspend fun <R, T : Request<R>> send(request: T): R {
    val handler by inject<RequestHandler<T, R>>(RequestHandler::class.java)
    return handler.handle(request)
}


suspend fun <R, T : Request<R>> sendWithValidation(request: T): R {
    val handler by inject<RequestHandler<T, R>>(RequestHandler::class.java)

    val validator by injectOrNull<Validator<Request<R>>>(Validator::class.java)
    validator?.validate(request)


    return handler.handle(request)
}

// 1. Define Request and Handler interfaces
interface Request<R>
interface RequestHandler<T : Request<R>, R> {
    suspend fun handle(request: T): R
}


interface RequestValue<T> : Request<ValueOr<T>>




fun <T> T.success(): Success<T> = Success(this)

fun ErrorI.failure() : ValueOr<Nothing> = Failure(listOf(this))
fun Iterable<ErrorI>.failure(): ValueOr<Nothing> = Failure(this.toList())

sealed interface ValueOr<out TSuccess>
data class Success<T>(val value: T) : ValueOr<T>
data class Failure(val errors: List<ErrorI>) : ValueOr<Nothing>


data class ErrorI(
    val errorType: Int,
    val code: String,
    val message: String? = null,
    val extensions: Map<String, Any>? = null
) {
    companion object {
        fun validation(code: String, message: String, extensions: Map<String, Any>? = null) =
            ErrorI(400, code, message, extensions)

        fun conflict(code: String, message: String, extensions: Map<String, Any>? = null) =
            ErrorI(409, code, message, extensions)

        fun notFound(code: String, message: String, extensions: Map<String, Any>? = null) =
            ErrorI(404, code, message, extensions)
    }
}

object ErrorTypes {
    const val validation = 400
    const val unauthorized = 401
    const val forbidden = 403
    const val notFound = 404
    const val conflict = 409
    const val server = 500
}

/*data class Error2(val errorType: ErrorType, val code: String, val message: String, val extensions: Map<String, Any>)
enum class ErrorType(val code: Int) {
    Unauthorized(401),
    Forbidden(403),
    NotFound(404),
    Conflict(409),
    Validation(400),
    Server(500),
}*/


/*suspend fun <T, R, E> ValueOr<T>.match( onSuccess: suspend (T) -> R, onError: suspend (List<Error>) -> E) {
    when (this) {
        is Success -> onSuccess(value)
        is Failure -> onError(errors)
    }
}*/

/*
interface Mediator {
    suspend fun <R, T : Request<R>> send(request: T): R
}


class MediatorImpl(private val koin: Koin) : Mediator {
    @Suppress("UNCHECKED_CAST")
    override suspend fun <R, T : Request<R>> send(request: T): R {
        val handler = koin.get<RequestHandler<T, R>>()
        return handler.handle(request)
    }
}
*/

