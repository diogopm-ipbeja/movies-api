package pt.ipbeja.moviesapi.utilities

import io.ktor.http.*
import kotlinx.datetime.LocalDate

fun Parameters.getLocalDate(key: String) : LocalDate? {
    val input = this[key] ?: return null
    return try {
        LocalDate.parse(input)
    }catch (_: Exception) {
        null
    }
}