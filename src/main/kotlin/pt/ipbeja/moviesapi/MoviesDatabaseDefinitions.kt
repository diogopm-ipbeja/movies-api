package pt.ipbeja.moviesapi

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import pt.ipbeja.moviesapi.entities.users.UserPictureInfo
import pt.ipbeja.moviesapi.serialization.appJson
import kotlin.time.Clock


object Users : IntIdTable("users") {
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 128)
    val role = varchar("role", 15) // e.g., "ADMIN" or "USER"
    val dateOfBirth = date("date_of_birth").nullable()
    val picture = text("picture").nullable()
    // val picture2 = jsonb<UserPictureInfo>("picture2",appJson).nullable()
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val updatedAt = timestamp("updated_at").nullable()
}

object Movies : IntIdTable("movies") {
    val title = varchar("title", 255)
    val synopsis = text("synopsis")
    val minimumAge = integer("minimumAge").check("minimum_age_constraint") { (it greaterEq 0) and (it lessEq 18) }
    val releaseDate = date("release_date")
    val director = reference("director", Persons).nullable()
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val updatedAt = timestamp("updated_at").nullable()
}

object MovieGenres: CompositeIdTable("movie_genres") {
    val movie = reference("movie_id", Movies)
    val genre = reference("genre_id", Genres)
    override val primaryKey: PrimaryKey = PrimaryKey(movie, genre )
}

object Genres: IntIdTable("genres") {
    val name = varchar("name", 15).uniqueIndex()
    val description = varchar("description", 255).nullable()
}

object Persons : IntIdTable("persons") {
    val name = varchar("name", 255)
    val dateOfBirth = date("date_of_birth").nullable() // may be unknown?
}

object MoviePictures : IntIdTable("movie_pictures") {
    val movie = reference("movie_id", Movies, onDelete = ReferenceOption.CASCADE)
    val url = varchar("url", 255)
    val filename = varchar("filename", 63)
    val contentType = varchar("content_type", 32)
    val description = text("description").nullable()
    val mainPicture = bool("main_picture")
}

object PersonPictures : IntIdTable("person_pictures") {
    val person = reference("person_id", Persons, onDelete = ReferenceOption.CASCADE)
    val url = varchar("url", 255)
    val filename = varchar("filename", 63)
    val contentType = varchar("content_type", 32)
    val description = text("description").nullable()
    val mainPicture = bool("main_picture")
}

object CastMembers : CompositeIdTable("cast_members") {
    val movie = reference("movie_id", Movies, onDelete = ReferenceOption.CASCADE)
    val person = reference("person_id", Persons, onDelete = ReferenceOption.CASCADE)
    val role = text("role").entityId()
    // val designation = text("designation")
    // val asd = enumerationByName<Roles>("sdad", 32)
    // val asd2 = customEnumeration("columnName",{ value -> enumValueOf<Roles>(value as String) }, { PGEnum("roles", it) })
    // val asd2 = customEnumeration("enumCol", "roles", fromDb = { v -> enumValueOf<Roles>(v as String)}, toDb = { v -> PGEnum("roles", v)})


    override val primaryKey: PrimaryKey = PrimaryKey(movie, person, role)
}

object Favorites : CompositeIdTable("favorites") {
    val user = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val movie = reference("movie_id", Movies, onDelete = ReferenceOption.CASCADE)
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }

    override val primaryKey: PrimaryKey = PrimaryKey(user, movie)
}

object MovieRatings : CompositeIdTable("movie_ratings") {
    val user = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val movie = reference("movie_id", Movies, onDelete = ReferenceOption.CASCADE)
    val score = integer("score")
    val comment = varchar("comment", 1023).nullable()
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val updatedAt = timestamp("updated_at").nullable()

    init {
        addIdColumn(user)
        addIdColumn(movie)
    }

    override val primaryKey: PrimaryKey = PrimaryKey(user, movie)

}


class UserEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserEntity>(Users)
    var username by Users.username
    var passwordHash by Users.passwordHash
    var role by Users.role
    var picture by Users.picture
    var dateOfBirth by Users.dateOfBirth
    val createdAt by Users.createdAt
    var updatedAt by Users.updatedAt
    val favorites by MovieEntity via Favorites
    val ratedMovies by MovieRatingEntity referrersOn MovieRatings.user

}

class MovieEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<MovieEntity>(Movies)
    var title by Movies.title
    var synopsis by Movies.synopsis
    var releaseDate by Movies.releaseDate
    var director by PersonEntity optionalReferencedOn Movies.director
    var directorId by Movies.director
    var createdAt by Movies.createdAt
    var updatedAt by Movies.updatedAt
    val pictures by MoviePictureEntity referrersOn MoviePictures.movie
    val castMembers by CastMemberEntity referrersOn CastMembers.movie
    val favoritedBy by UserEntity via Favorites
    val ratings by MovieRatingEntity referrersOn MovieRatings.movie

    val genres by GenreEntity via MovieGenres
    var minimumAge by Movies.minimumAge
}

// class GenreEntity(id: EntityID<String>) : Entity<String>(id) {
//     companion object : EntityClass<String, GenreEntity>(Genres)
class GenreEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<GenreEntity>(Genres)
    var name by Genres.name
    var description by Genres.description
    val movies by MovieEntity via MovieGenres
}

class PersonEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PersonEntity>(Persons)
    var name by Persons.name
    var dateOfBirth by Persons.dateOfBirth
    val rolesInMovies by CastMemberEntity referrersOn CastMembers.person
    val directedMovies by MovieEntity optionalReferrersOn Movies.director
    val pictures by PersonPictureEntity referrersOn PersonPictures.person
}

class MoviePictureEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<MoviePictureEntity>(MoviePictures)
    var movie by MovieEntity referencedOn MoviePictures.movie
    var filename by MoviePictures.filename
    var contentType by MoviePictures.contentType
    var url by MoviePictures.url
    var mainPicture by MoviePictures.mainPicture
    var description by MoviePictures.description
}

class PersonPictureEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PersonPictureEntity>(PersonPictures)
    var person by PersonEntity referencedOn PersonPictures.person
    var filename by PersonPictures.filename
    var contentType by PersonPictures.contentType
    var url by PersonPictures.url
    var mainPicture by PersonPictures.mainPicture
    var description by PersonPictures.description
}

class CastMemberEntity(id: EntityID<CompositeID>) : CompositeEntity(id) {
    companion object : CompositeEntityClass<CastMemberEntity>(CastMembers)

    var movie by MovieEntity referencedOn CastMembers.movie
    var person by PersonEntity referencedOn CastMembers.person
    var personId by CastMembers.person
    var movieId by CastMembers.movie
    var role by CastMembers.role // character played
}

class FavoriteEntity(id: EntityID<CompositeID>) : CompositeEntity(id) {
    companion object : CompositeEntityClass<CastMemberEntity>(Favorites)

    val createdAt by Favorites.createdAt

    var user by UserEntity referencedOn Favorites.user
    var movie by MovieEntity referencedOn Favorites.movie

}

class MovieRatingEntity(id: EntityID<CompositeID>) : CompositeEntity(id) {
    companion object : CompositeEntityClass<MovieRatingEntity>(MovieRatings)
    var user by UserEntity referencedOn MovieRatings.user
    var userId by MovieRatings.user
    var movie by MovieEntity referencedOn MovieRatings.movie
    var movieId by MovieRatings.movie
    var score by MovieRatings.score
    var comment by MovieRatings.comment
    val createdAt by MovieRatings.createdAt
    var updatedAt by MovieRatings.updatedAt
}