package pt.ipbeja.moviesapi.entities.movies.model


data class PictureInfo(val id: Int, val filename: String, val contentType: String, val mainPicture: Boolean, val description: String?)

data class RoleAssignment(val personId: Int, val role: String)

