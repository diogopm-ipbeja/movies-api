package pt.ipbeja.moviesapi.entities.people

import kotlinx.datetime.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pt.ipbeja.moviesapi.PersonEntity
import pt.ipbeja.moviesapi.entities.common.CreatePictureRequest
import pt.ipbeja.moviesapi.entities.common.PictureInfoResponse
import pt.ipbeja.moviesapi.entities.common.toCreateDefinition
import pt.ipbeja.moviesapi.entities.common.toResponse
import pt.ipbeja.moviesapi.entities.people.usecases.commands.CreatePersonCommand
import java.util.*


@Serializable
data class CreatePersonRequest(val name: String, val dateOfBirth: LocalDate?, val pictures: List<CreatePictureRequest>)

@Serializable
data class PersonResponse(val id: Int, val name: String, val dateOfBirth: LocalDate?, val picture: PictureInfoResponse? = null)

@Serializable
data class PersonDetailResponse(val id: Int, val name: String, val dateOfBirth: LocalDate?, val pictures: List<PictureInfoResponse>, val directedMovies: List<Directed>, val roles: List<Role>) {

    @Serializable
    data class Directed(val id: Int, val title: String, val releaseDate: LocalDate, val picture: PictureInfoResponse?)

    @Serializable
    data class Role(val movieId: Int, val title: String, val releaseDate: LocalDate, val character: String)
}



@Serializable
data class UpdatePersonRequest(val id: Int, val name: String, val dateOfBirth: LocalDate?)




fun CreatePersonRequest.toCommand() = CreatePersonCommand(name, dateOfBirth, pictures.toCreateDefinition())

fun PersonDetail.toResponse() = PersonDetailResponse(id, name, dateOfBirth, pictures.map { it.toResponse() }, directedMovies.map { it.toResponse() }, roles.map { it.toResponse() })


fun PersonDetail.Directed.toResponse() = PersonDetailResponse.Directed(id, title, releaseDate, picture?.toResponse())
fun PersonDetail.Role.toResponse() = PersonDetailResponse.Role(movieId, title, releaseDate, character)
fun Person.toResponse() = PersonResponse(id, name, dateOfBirth, (pictures.firstOrNull { it.mainPicture } ?: pictures.firstOrNull())?.toResponse())
fun PersonEntity.toResponse() = PersonResponse(
    id.value,
    name,
    dateOfBirth
)





@Serializable(with = PatchPersonSerializer::class)
data class PatchPersonRequest(val id: Int, val name: Optional<String>, val dateOfBirth: Optional<LocalDate>)


object PatchPersonSerializer : KSerializer<PatchPersonRequest> {
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("PersonPatchSerializer")

    override fun serialize(encoder: Encoder, value: PatchPersonRequest) {
        TODO("Not yet implemented")
    }

    override fun deserialize(decoder: Decoder): PatchPersonRequest {
        require(decoder is JsonDecoder)

        val patch = decoder.decodeJsonElement().jsonObject

        val id = patch["id"]?.jsonPrimitive?.int!!
        val name = patch["name"]?.let { Optional.of(it.jsonPrimitive.content) } ?: Optional.empty<String>()
        val dob = patch["dateOfBirth"]?.let { Optional.of(LocalDate.parse(it.jsonPrimitive.content)) } ?: Optional.empty<LocalDate>()

        return PatchPersonRequest(
            id,
            name,
            dob
        )

    }
}