package pt.ipbeja.moviesapi.entities.people.usecases.queries

import kotlinx.datetime.LocalDateRange
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.leftJoin
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.CastMembers
import pt.ipbeja.moviesapi.PersonEntity
import pt.ipbeja.moviesapi.Persons
import pt.ipbeja.moviesapi.entities.common.findMainPicture
import pt.ipbeja.moviesapi.entities.common.toPictureInfo
import pt.ipbeja.moviesapi.entities.people.PersonDetail
import pt.ipbeja.moviesapi.entities.people.PersonSimple
import pt.ipbeja.moviesapi.utilities.*

data class GetPersonQuery(val personId: Int): Request<ValueOr<PersonDetail>>

class GetPersonQueryHandler(val db: Database) : RequestHandler<GetPersonQuery, ValueOr<PersonDetail>> {

    override suspend fun handle(request: GetPersonQuery): ValueOr<PersonDetail> = transaction(db) {
        val person = PersonEntity.findById(request.personId) ?: return@transaction ErrorI.notFound("Person.NotFound", "Person with id '${request.personId}' does not exist.").failure()
        val roles = person.rolesInMovies.notForUpdate()
        val directed = person.directedMovies.notForUpdate()

        PersonDetail(
            person.id.value,
            person.name,
            person.dateOfBirth,
            person.pictures.map { it.toPictureInfo() },
            directed.map {
                val mainPic = it.pictures.findMainPicture()
                PersonDetail.Directed(it.id.value, it.title, it.releaseDate, mainPic?.toPictureInfo())
            },
            roles.map { PersonDetail.Role(it.movie.id.value, it.movie.title, it.movie.releaseDate, it.role) })
            .success()
    }
}


data object GetPeopleQuery : Request<List<PersonSimple>>
// data class GetPeopleQuery2(val nameLike: String, val sortOrder: String = "asc", val sortBy: String = "name") : Request<List<PersonSimple>>

class GetPeopleQueryHandler(private val db: Database) : RequestHandler<GetPeopleQuery, List<PersonSimple>> {
    override suspend fun handle(request: GetPeopleQuery): List<PersonSimple> = transaction(db) {
        PersonEntity.all()
            .map {
                PersonSimple(it.id.value, it.name, it.dateOfBirth, it.pictures.findMainPicture()?.toPictureInfo())
            }
    }
}



class QueryActorsUseCase(val db: Database) {

    fun handle(dobRange: LocalDateRange?, nameLike: String?) = transaction(db) {


        // PersonEntity.all().notForUpdate().any {  it.rolesInMovies.any { r -> r.movie.genres.notForUpdate().any { g -> g.id.value in listOf<Int>() } }}
        var query = Persons
            .leftJoin(CastMembers, onColumn = { Persons.id }, otherColumn = { CastMembers.person })
            .selectAll()
            .withDistinctOn( CastMembers.person)
        if (dobRange != null) {
            query = query.where { Persons.dateOfBirth neq null }
                .andWhere { Persons.dateOfBirth greaterEq dobRange.first }
                .andWhere { Persons.dateOfBirth lessEq dobRange.last }
        }
        if (!nameLike.isNullOrBlank()) {
            query = query.andWhere { Persons.name ilike nameLike }
        }

    }

}


class QueryDirectorsUseCase(val db: Database) {

    fun handle() = transaction(db) {

        PersonEntity.all().notForUpdate().any {
            it.rolesInMovies.any { r ->
                r.movie.genres.notForUpdate().any { g -> g.id.value in listOf<Int>() }
            }
        }


    }

}
