package pt.ipbeja.moviesapi.entities.genres.model

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import pt.ipbeja.moviesapi.GenreEntity
import pt.ipbeja.moviesapi.Genres


data class Genre(val id: Int, val name: String, val description: String?)
data class GenreStatistics(val id: Int, val name: String, val averageRating: Float)

fun GenreEntity.toGenre() = Genre(this.id.value, this.name, this.description)
fun ResultRow.toGenre() = Genre(this[Genres.id].value, this[Genres.name], this[Genres.description])

fun SizedIterable<GenreEntity>.toGenres() = map { it.toGenre()}
