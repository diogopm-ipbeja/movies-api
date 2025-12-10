package pt.ipbeja.moviesapi.entities.people

import kotlinx.datetime.LocalDate
import pt.ipbeja.moviesapi.entities.movies.model.PictureInfo

class PersonSimple(val id: Int, val name: String, val dateOfBirth: LocalDate?, val picture: PictureInfo?)
class Person(val id: Int, val name: String, val dateOfBirth: LocalDate?, val pictures: List<PictureInfo>)
class PersonDetail(
    val id: Int,
    val name: String,
    val dateOfBirth: LocalDate?,
    val pictures: List<PictureInfo>,
    val directedMovies: List<Directed>,
    val roles: List<Role>
) {
    class Directed(val id: Int, val title: String, val releaseDate: LocalDate, val picture: PictureInfo?)
    class Role(val movieId: Int, val title: String, val releaseDate: LocalDate, val character: String)
}