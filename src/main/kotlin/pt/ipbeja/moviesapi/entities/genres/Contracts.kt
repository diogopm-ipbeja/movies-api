package pt.ipbeja.moviesapi.entities.genres

import kotlinx.serialization.Serializable
import pt.ipbeja.moviesapi.entities.genres.model.Genre
import pt.ipbeja.moviesapi.entities.genres.usecases.commands.CreateGenre
import pt.ipbeja.moviesapi.entities.genres.usecases.commands.CreateGenresCommand

/**
 * @author Diogo Pina Manique
 * @version 02/12/2025
 */


@Serializable
data class CreateGenreRequest(val name: String, val description: String?)

@Serializable
data class UpdateGenreRequest(val id: Int, val name: String, val description: String?)

@Serializable
data class GenreResponse(val id: Int, val name: String, val description: String?)


fun Set<Genre>.toResponse() = map { it.toResponse() }.toSet()


fun CreateGenreRequest.toCommand() = CreateGenresCommand(listOf(CreateGenre(name, description)))

fun List<CreateGenreRequest>.toCommand() = CreateGenresCommand(map {
    CreateGenre(it.name, it.description)
})