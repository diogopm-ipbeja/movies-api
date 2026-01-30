package pt.ipbeja.moviesapi.entities.common

import io.ktor.util.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import pt.ipbeja.moviesapi.MoviePictureEntity
import pt.ipbeja.moviesapi.MoviePictures
import pt.ipbeja.moviesapi.PersonPictureEntity
import pt.ipbeja.moviesapi.entities.movies.model.PictureInfo
import java.io.File

class CreatePicture(val filename: String, val data: ByteArray, val description: String? = null, val mainPicture: Boolean = false)


@Serializable
data class CreatePictureRequest(val filename: String, val data: String, val description: String? = null, val mainPicture : Boolean = false)

@Serializable
data class CreateUserPictureRequest(val filename: String, val data: String, val description: String? = null, val mainPicture : Boolean = false)


class FileRepresentation(val file: File, val filename: String, val contentType: String)



fun CreatePictureRequest.toCreateDefinition() = CreatePicture(filename, data.decodeBase64Bytes(), description, mainPicture)
fun List<CreatePictureRequest>.toCreateDefinition() = map { it.toCreateDefinition() }



@Serializable
data class PictureInfoResponse(val id: Int, val mainPicture: Boolean, val filename: String, val contentType: String, val description: String?)


fun PictureInfo.toResponse() = PictureInfoResponse(id, mainPicture, filename, contentType, description)
fun List<PictureInfo>.toResponse() = map { it.toResponse() }


fun SizedIterable<MoviePictureEntity>.findMainPicture(): MoviePictureEntity? {
    var candidate: MoviePictureEntity? = null

    for (pic in this) {
        if(pic.mainPicture) return pic
        if(candidate == null) candidate = pic
    }
    return candidate
}


fun SizedIterable<PersonPictureEntity>.findMainPicture(): PersonPictureEntity? {
    var candidate: PersonPictureEntity? = null

    for (pic in this) {
        if(pic.mainPicture) return pic
        if(candidate == null) candidate = pic
    }
    return candidate
}


fun PersonPictureEntity.toPictureInfo() = PictureInfo(this.id.value, filename, contentType, true, description)
fun MoviePictureEntity.toPictureInfo() = PictureInfo(this.id.value, filename, contentType, true, description)