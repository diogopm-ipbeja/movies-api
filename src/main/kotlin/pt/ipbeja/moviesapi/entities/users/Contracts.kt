package pt.ipbeja.moviesapi.entities.users

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import pt.ipbeja.moviesapi.entities.common.CreatePictureRequest
import kotlin.time.Instant

@Serializable
data class RegisterUserRequest(val username: String, val password: String, val dateOfBirth: LocalDate? = null, val picture: CreatePictureRequest? = null)

@Serializable
data class UpdateUserRequest(val firstName: String, val lastName: String, val dateOfBirth: LocalDate?)

@Serializable
data class ChangeUserPasswordRequest(val currentPassword: String, val newPassword: String)

@Serializable
data class UserResponse(val id: Int, val username: String, val createdAt: Instant, val updatedAt: Instant?)

@Serializable
data class PrivateUserResponse(val id: Int, val username: String, val dateOfBirth: LocalDate?, val createdAt: Instant, val updatedAt: Instant?)


@Serializable
data class LoginResponse(val id: Int, val username: String, val role: String)