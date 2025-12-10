package pt.ipbeja.moviesapi.entities.people.usecases.commands

import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.PersonEntity
import pt.ipbeja.moviesapi.PersonPictureEntity
import pt.ipbeja.moviesapi.PersonPictures
import pt.ipbeja.moviesapi.entities.common.CreatePicture
import pt.ipbeja.moviesapi.entities.movies.model.PictureInfo
import pt.ipbeja.moviesapi.entities.people.Person
import pt.ipbeja.moviesapi.services.StorageService
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler
import pt.ipbeja.moviesapi.utilities.getMimeType

data class UpdatePersonCommand(val id: Int, val name: String, val dateOfBirth: LocalDate?) : Request<Person>
class UpdatePersonCommandHandler(val db: Database) : RequestHandler<UpdatePersonCommand, Person>{


    override suspend fun handle(request: UpdatePersonCommand) = transaction(db) {


        val person = PersonEntity.Companion.findByIdAndUpdate(request.id) {
            it.name = request.name.trim()
            it.dateOfBirth = request.dateOfBirth
        } ?: throw Exception()


        val pictures = person.pictures.map {
            PictureInfo(
                it.id.value,
                it.filename,
                it.contentType,
                it.mainPicture,
                it.description
            )
        }
        Person(person.id.value, person.name, person.dateOfBirth, pictures)
    }
}

data class AddPicturesToPersonCommand(val personId: Int, val pictures: List<CreatePicture>) : Request<Person>

class AddPicturesToPersonCommandHandler(val db: Database, val storageService: StorageService) : RequestHandler<AddPicturesToPersonCommand, Person> {


    override suspend fun handle(request: AddPicturesToPersonCommand) = transaction(db) {

        val personId = request.personId
        val pictures = request.pictures

        val defaultPicture = request.pictures.firstOrNull { it.mainPicture }

        if(defaultPicture != null) {

            PersonPictureEntity.Companion.findSingleByAndUpdate(op =  (PersonPictures.person eq personId) and (PersonPictures.mainPicture eq true)) {
                it.mainPicture = false
            }
        }

        val tx = storageService.createTransaction("person_$personId")


        val storedPictures = pictures.map { it to tx.addFile(it.filename, it.data) }


        /*val picInfo = */PersonPictures.batchInsert(storedPictures) {
            this[PersonPictures.person] = personId
            this[PersonPictures.mainPicture] = it.first === defaultPicture
            this[PersonPictures.filename] = it.first.filename
            this[PersonPictures.url] = it.second.toString()
            this[PersonPictures.contentType] = it.second.getMimeType()
        }/*.map {
            PictureInfo(it[PersonPictures.id].value, it[PersonPictures.filename], it[PersonPictures.contentType], it[PersonPictures.mainPicture])
        }*/

        val person = PersonEntity.Companion[personId]
        Person(
            person.id.value,
            person.name,
            person.dateOfBirth,
            person.pictures.map {
                PictureInfo(
                    it.id.value,
                    it.filename,
                    it.contentType,
                    it.mainPicture,
                    it.description
                )
            })
    }
}


data class RemovePicturesFromPersonCommand(val personId: Int, val pictures: Set<Int>) : Request<Person>

class RemovePicturesFromPersonCommandHandler(val db: Database, val storageService: StorageService) : RequestHandler<RemovePicturesFromPersonCommand, Person> {


    override suspend fun handle(request: RemovePicturesFromPersonCommand) = transaction(db) {

        val personId = request.personId
        val pictures = request.pictures

        val person = PersonEntity.Companion[personId]

        val toRemove = person.pictures.filter { it.id.value in pictures }.toSet()

        val removedMainPic = toRemove.firstOrNull { it.mainPicture }

            val remaining = person.pictures - toRemove
        if(removedMainPic != null) {
            remaining.first().mainPicture = true
        }


        val tx = storageService.createTransaction("")

        toRemove.forEach {
            tx.removeFile(it.url)
            it.delete()
        }

        tx.commit()
        Person(
            person.id.value,
            person.name,
            person.dateOfBirth,
            remaining.map { PictureInfo(it.id.value, it.filename, it.contentType, it.mainPicture, it.description) })
    }


}