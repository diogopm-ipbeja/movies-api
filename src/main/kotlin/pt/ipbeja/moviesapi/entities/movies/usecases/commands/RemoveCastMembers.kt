package pt.ipbeja.moviesapi.entities.movies.usecases.commands

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteIgnoreWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.CastMembers
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler

data class RemoveCastMembersCommand(val movieId: Int, val members: Set<Int>) : Request<Int>

class RemoveCastMembersCommandHandler(val db: Database) : RequestHandler<RemoveCastMembersCommand, Int> {

    override suspend fun handle(request: RemoveCastMembersCommand) = transaction(db) {
        CastMembers.deleteIgnoreWhere { (CastMembers.movie eq request.movieId) and (CastMembers.person inList request.members) }
    }
}