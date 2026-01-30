package pt.ipbeja.moviesapi.entities.movies.usecases.queries

import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.*
import pt.ipbeja.moviesapi.entities.common.findMainPicture
import pt.ipbeja.moviesapi.entities.common.toPictureInfo
import pt.ipbeja.moviesapi.entities.movies.model.Director
import pt.ipbeja.moviesapi.entities.movies.model.MovieSimple
import pt.ipbeja.moviesapi.entities.movies.model.PictureInfo
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler
import pt.ipbeja.moviesapi.utilities.ilike
import kotlin.time.Instant

data class GetMoviesQuery(
    val offset: Long = 0,
    val count: Int = Int.MAX_VALUE,
    val director: Int? = null,
    val genre: String? = null,
    val title: String? = null,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val rating: IntRange? = null,
    val favoritesOnly: Boolean = false,
    val sortBy: String = "releaseDate",
    val sortOrder: String = "desc",
    val userId: Int? = null
) : Request<List<MovieSimple>>


class GetMoviesQueryHandler(val db: Database) : RequestHandler<GetMoviesQuery, List<MovieSimple>> {


    private data class MovieQueryResult(
        val id: Int,
        val title: String,
        val synopsis: String,
        val releaseDate: LocalDate,
        val directorId: Int?,
        val rating: Float?,
        val createdAt: Instant,
        val updatedAt: Instant?,
        val favorite: Boolean = false,
    )


    override suspend fun handle(request: GetMoviesQuery): List<MovieSimple> = transaction(db) {
        addLogger(StdOutSqlLogger)
        val conditions = mutableListOf<Op<Boolean>>()

        val requesterId = if(request.userId != null && request.userId > 0) request.userId else null

        val sortOrder =
            if (request.sortOrder.equals("asc", true)) SortOrder.ASC_NULLS_FIRST else SortOrder.DESC_NULLS_LAST
        request.fromDate?.let { conditions += (Movies.releaseDate greaterEq it) }
        request.toDate?.let { conditions += (Movies.releaseDate lessEq it) }

        request.title?.let {
            conditions += Movies.title ilike "%${it}%"
        }
        request.director?.let {
            conditions += Movies.director eq it
        }




        val userEntity = requesterId?.let { UserEntity[it] }
        val results = if (request.sortBy.equals("rating", true)) {

            if (requesterId != null && request.favoritesOnly) {
                conditions += Favorites.user eq requesterId
            }

            val finalCondition = if (conditions.isEmpty()) {
                Op.TRUE
            } else {
                conditions.reduce { acc, op -> acc and op }
            }

            val avgScore = MovieRatings.score.avg().alias("average")

            val seenMovieCol = MovieRatings.movie

            // val coalescedAvg = coalesce(SeenMovies.score.avg(), floatLiteral(-1f)).alias("average")

            // val coalescedFav = coalesce(Favorites.user, intLiteral(0)).alias("favorited")
            val countFav = Favorites.user.count().greater(0).alias("fav_count")
            val intermediate = Movies
                .leftJoin(MovieRatings, onColumn = { Movies.id }, otherColumn = { seenMovieCol })
                .leftJoin(Favorites, onColumn = { Movies.id }, otherColumn = { movie })
                // .leftJoin(Persons, onColumn = { Movies.director }, otherColumn = { Persons.id })
                .select(
                    Movies.id,
                    Movies.releaseDate,
                    Movies.title,
                    Movies.synopsis,
                    seenMovieCol,
                    avgScore,
                    Movies.director,
                    Movies.createdAt,
                    Movies.updatedAt,
                    countFav

                ).offset(request.offset).limit(request.count)
                .groupBy(Movies.id, seenMovieCol)
                .orderBy(avgScore to sortOrder)
                .where(finalCondition)
                .map {
                    MovieQueryResult(
                        it[Movies.id].value,
                        it[Movies.title],
                        it[Movies.synopsis],
                        it[Movies.releaseDate],
                        it[Movies.director]?.value,
                        it[avgScore]?.toFloat(),
                        it[Movies.createdAt],
                        it[Movies.updatedAt],
                        it[countFav]
                    )
                }



            intermediate
                .map {
                    val genres =
                        MovieGenres.leftJoin(Genres, onColumn = { MovieGenres.genre }, otherColumn = { Genres.id })
                            .selectAll()
                            .where { MovieGenres.movie eq it.id }
                            .map { g ->
                                g[Genres.name]
                            }.toSet()


                    if (!request.genre.isNullOrBlank() and !genres.any { g -> g.equals(request.genre, true) }) {
                        return@map null
                    }


                    val director = it.directorId?.let { dirId -> PersonEntity[dirId] }


                    val directorPics = director?.pictures?.toList()
                    val directorMainPic =
                        directorPics?.firstOrNull { p -> p.mainPicture } ?: directorPics?.firstOrNull()

                    val dirPic = directorMainPic?.let { p ->
                        PictureInfo(p.id.value, p.filename, p.contentType, p.mainPicture, p.description)
                    }


                    val mainPicInfo = MoviePictureEntity.find { MoviePictures.movie eq it.id }
                        .findMainPicture()?.toPictureInfo()
                    /* val pictures = MoviePictures.selectAll().where { MoviePictures.movie eq it.id }.map { row ->
                         row[MoviePictures.id] to row[MoviePictures.mainPicture]
                     }*/

                    // val mainPicture = pictures.firstOrNull { p -> p.second }?.first ?: pictures.firstOrNull()?.first

                    MovieSimple(
                        it.id,
                        it.title,
                        it.synopsis,
                        genres,
                        it.releaseDate,
                        mainPicInfo,
                        userEntity?.favorites?.notForUpdate()?.any { um -> um.id.value == it.id } ?: false,
                        director?.let { p -> Director(p.id.value, p.name, dirPic) },
                        it.rating,
                        it.createdAt,
                        it.updatedAt
                    )

                }

        } else {


            val finalCondition = if (conditions.isEmpty()) {
                Op.TRUE
            } else {
                conditions.reduce { acc, op -> acc and op }
            }

            val sortColumn = when (request.sortBy) {
                "title" -> Movies.title
                else -> Movies.releaseDate
            }

            MovieEntity.find { finalCondition }
                .orderBy(sortColumn to sortOrder)
                .filter {
                    (request.genre.isNullOrBlank() or it.genres.any { g -> g.name.equals(request.genre, true) }) and ((requesterId == null) or !request.favoritesOnly or it.favoritedBy.any { f -> f.id.value == requesterId })
                }
                .map {
                    val scores = it.ratings.map { seen -> seen.score }
                    val averageScore = if (scores.isEmpty()) null else scores.average().toFloat()


                    val director = it.director?.let { p ->
                        val directorPicture = p.pictures.firstOrNull { p -> p.mainPicture } ?: p.pictures.firstOrNull()
                        val dirPic = directorPicture?.let { pic ->
                            PictureInfo(pic.id.value, pic.filename, pic.contentType, pic.mainPicture, pic.description)
                        }
                        Director(p.id.value, p.name, dirPic)
                    }


                    val moviePicture = it.pictures.firstOrNull { p -> p.mainPicture } ?: it.pictures.firstOrNull()
                    MovieSimple(
                        it.id.value,
                        it.title,
                        it.synopsis,
                        it.genres.map { g -> g.name }.toSet(),
                        it.releaseDate,
                        moviePicture?.toPictureInfo(),
                        userEntity?.favorites?.notForUpdate()?.any { um -> um.id.value == it.id.value } ?: false,
                        director,
                        averageScore,
                        it.createdAt,
                        it.updatedAt
                    )

                }
        }
        results.filter { it != null }.map { it!! }
    }

}