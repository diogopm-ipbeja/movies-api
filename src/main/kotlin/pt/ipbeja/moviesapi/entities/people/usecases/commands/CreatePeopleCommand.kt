package pt.ipbeja.moviesapi.entities.people.usecases.commands


import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.PersonEntity
import pt.ipbeja.moviesapi.PersonPictures
import pt.ipbeja.moviesapi.entities.common.CreatePicture
import pt.ipbeja.moviesapi.entities.movies.model.PictureInfo
import pt.ipbeja.moviesapi.entities.people.Person
import pt.ipbeja.moviesapi.services.StorageService
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler
import pt.ipbeja.moviesapi.utilities.getMimeType


data class CreatePersonCommand(val name: String, val dateOfBirth: LocalDate?, val pictures: List<CreatePicture>) :
    Request<Person>



class CreatePersonCommandHandler(val db: Database, val storageService: StorageService) :
    RequestHandler<CreatePersonCommand, Person> {


    override suspend fun handle(request: CreatePersonCommand) = transaction(db) {

        createPerson(storageService, request.name, request.dateOfBirth, request.pictures)
    }
}


data class CreatePeopleCommand(val people: List<CreatePersonCommand>) : Request<List<Person>>


class CreatePeopleCommandHandler(val db: Database, val storageService: StorageService) :
    RequestHandler<CreatePeopleCommand, List<Person>> {

    override suspend fun handle(request: CreatePeopleCommand): List<Person> = transaction(db) {
        request.people.map { createPerson(storageService, it.name, it.dateOfBirth, it.pictures) }
    }
}

private fun createPerson(
    storageService: StorageService,
    name: String,
    dateOfBirth: LocalDate?,
    pictures: List<CreatePicture>
): Person {
    val person = PersonEntity.Companion.new {
        this.name = name.trim()
        this.dateOfBirth = dateOfBirth
    }

    assert(pictures.count { it.mainPicture } < 2)


    val pictures = if (pictures.isNotEmpty()) {
        val tx = storageService.createTransaction("person_${person.id.value}")

        val defaultPicture = pictures.firstOrNull { it.mainPicture } ?: pictures.firstOrNull()

        val picAndPath = pictures.map {
            it to tx.addFile(it.filename, it.data)
        }


        PersonPictures.batchInsert(picAndPath) {
            this[PersonPictures.person] = person.id
            this[PersonPictures.url] = it.second.toString()
            this[PersonPictures.filename] = it.first.filename
            this[PersonPictures.mainPicture] = it.first === defaultPicture
            this[PersonPictures.contentType] = it.second.getMimeType()
            this[PersonPictures.description] = it.first.description
        }.map {
            PictureInfo(
                it[PersonPictures.id].value,
                it[PersonPictures.filename],
                it[PersonPictures.contentType],
                it[PersonPictures.mainPicture],
                it[PersonPictures.description]
            )
        }
    } else emptyList()

    return Person(person.id.value, person.name, person.dateOfBirth, pictures)
}

