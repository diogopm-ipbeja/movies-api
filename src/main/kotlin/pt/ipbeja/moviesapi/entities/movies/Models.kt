package pt.ipbeja.moviesapi.entities.movies

import kotlinx.serialization.Serializable


@Serializable
data class Rating(val average: Float, val buckets: List<RatingBucket>)

@Serializable
data class RatingBucket(val rating: Int, val count: Int)



enum class Roles {
    Direction,
    Cast
}