package pt.ipbeja.moviesapi.entities.movies.usecases.queries

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.entities.movies.Rating
import pt.ipbeja.moviesapi.entities.movies.RatingBucket
import pt.ipbeja.moviesapi.*
import pt.ipbeja.moviesapi.entities.genres.model.toGenre
import pt.ipbeja.moviesapi.entities.movies.model.*
import pt.ipbeja.moviesapi.utilities.*

data class GetMovieQuery(val movieId: Int, val requesterId: Int? = null) : Request<ValueOr<MovieDetail>>

class GetMovieQueryHandler(val database: Database) : RequestHandler<GetMovieQuery, ValueOr<MovieDetail>> {


    override suspend fun handle(request: GetMovieQuery): ValueOr<MovieDetail> = transaction {
        val movieId = request.movieId
        val requesterId = request.requesterId


        val movie = MovieEntity.findById(movieId) ?: return@transaction ErrorI(
            ErrorTypes.notFound,
            "Movie.NotFound",
            "Movie ${request.movieId} could not be found."
        ).failure()

        val scores = movie.ratings.map { seen -> seen.score }
        val averageScore = if(scores.isEmpty()) 0f else scores.average().toFloat()
        val ratingCounts = scores.groupBy { s -> s }.mapValues { v -> v.value.count() }

        val buckets = buildList {
            repeat(6) { i ->
                val count = ratingCounts.getOrDefault(i, 0)
                add(RatingBucket(i, count))
            }
        }



        val rating = if(ratingCounts.any { it.value > 0 }) Rating(averageScore, buckets) else null



        val director = movie.director?.let {
            val dirPic = it.pictures.firstOrNull { p -> p.mainPicture }?.let { p ->
                PictureInfo(p.id.value,  p.filename, p.contentType,p.mainPicture, p.description)
            }
            Director(it.id.value, it.name, dirPic) }

        var userRating : UserRating? = null
        var isFavorite = false
        if(requesterId != null) {

            userRating = MovieRatingEntity.find { (MovieRatings.user eq requesterId) and (MovieRatings.movie eq movieId) }.limit(1).firstOrNull()?.let {
                UserRating(it.score, it.comment)
            }
            isFavorite = FavoriteEntity.find { (Favorites.user eq requesterId) and (Favorites.movie eq movieId) }.limit(1).count() == 1L
        }

        val cast = movie.castMembers.map { CastMember(it.person.id.value, it.person.name, it.role.value) }


        /*val mainMoviePic = movie.pictures.notForUpdate().orderBy(MoviePictures.mainPicture to SortOrder.ASC, MoviePictures.id to SortOrder.ASC)
            .limit(1)
            .firstOrNull()?.id?.value*/

        val pics = movie.pictures.notForUpdate().toList()
        val mainPic = pics.firstOrNull { it.mainPicture } ?: pics.firstOrNull()

        val moviePics =  pics.map { PictureInfo(it.id.value, it.filename, it.contentType, it === mainPic, it.description) }

        return@transaction MovieDetail(
            movieId,
            movie.title,
            movie.synopsis,
            movie.genres.map { it.toGenre() }.toSet(),
            movie.releaseDate,
            director,
            moviePics,
            rating,
            isFavorite,
            userRating,
            movie.minimumAge,
            movie.createdAt,
            movie.updatedAt,
            cast
        ).success()

    }

    fun handle(movieId: Int, requesterId: Int? = null): MovieDetail = transaction(database) {



   /*     val map = Movies.selectAll().map { it[Movies.id].value to it[Movies.title] }

        val findById = MovieEntity.findById(movieId)*/



        val movie = MovieEntity[movieId]
        val scores = movie.ratings.map { seen -> seen.score }
        val averageScore = if(scores.isEmpty()) 0f else scores.average().toFloat()
        val ratingCounts = scores.groupBy { s -> s }.mapValues { v -> v.value.count() }

        val buckets = buildList {
            repeat(6) { i ->
                val count = ratingCounts.getOrDefault(i, 0)
                add(RatingBucket(i, count))
            }
        }

        val rating = if(ratingCounts.any { it.value > 0 }) Rating(averageScore, buckets) else null



        val director = movie.director?.let {
            val dirPic = it.pictures.firstOrNull { p -> p.mainPicture }?.let { p ->
                PictureInfo(p.id.value,  p.filename, p.contentType,p.mainPicture, p.description)
            }
            Director(it.id.value, it.name, dirPic) }

        var userRating : UserRating? = null
        var isFavorite = false
        if(requesterId != null) {

            userRating = MovieRatingEntity.find { (MovieRatings.user eq requesterId) and (MovieRatings.movie eq movieId) }.limit(1).firstOrNull()?.let {
                UserRating(it.score, it.comment)
            }
            isFavorite = FavoriteEntity.find { (Favorites.user eq requesterId) and (Favorites.movie eq movieId) }.limit(1).count() == 1L
        }

        val cast = movie.castMembers.map { CastMember(it.person.id.value, it.person.name, it.role.value) }


        /*val mainMoviePic = movie.pictures.notForUpdate().orderBy(MoviePictures.mainPicture to SortOrder.ASC, MoviePictures.id to SortOrder.ASC)
            .limit(1)
            .firstOrNull()?.id?.value*/

        val pics = movie.pictures.notForUpdate().toList()
        val mainPic = pics.firstOrNull { it.mainPicture } ?: pics.firstOrNull()

        val moviePics =  pics.map { PictureInfo(it.id.value, it.filename, it.contentType, it === mainPic, it.description) }

        return@transaction MovieDetail(
            movieId,
            movie.title,
            movie.synopsis,
            movie.genres.map { it.toGenre() }.toSet(),
            movie.releaseDate,
            director,
            moviePics,
            rating,
            isFavorite,
            userRating,
            movie.minimumAge,
            movie.createdAt,
            movie.updatedAt,
            cast
        )

    }

}