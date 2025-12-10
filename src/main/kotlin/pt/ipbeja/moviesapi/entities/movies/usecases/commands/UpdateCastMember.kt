package pt.ipbeja.moviesapi.entities.movies.usecases.commands

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.CastMemberEntity
import pt.ipbeja.moviesapi.CastMembers
import pt.ipbeja.moviesapi.entities.movies.model.CastMember
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler

data class UpdateCastMemberCommand(val movieId: Int, val castMember: Int, val role: String) : Request<CastMember?>

class UpdateCastMemberCommandHandler(val db: Database) : RequestHandler<UpdateCastMemberCommand, CastMember?>{

    override suspend fun handle(request: UpdateCastMemberCommand) = transaction(db) {

        val current = CastMemberEntity.find { (CastMembers.movie eq request.movieId) and (CastMembers.person eq request.castMember) }
            .limit(1)
            .firstOrNull()

        if(current == null) return@transaction null
        val name = current.person.name

        current.delete()
        CastMembers.insert {
            it[CastMembers.movie] = request.movieId
            it[CastMembers.person] = request.castMember
            it[CastMembers.role] = request.role
        }

        CastMember(request.castMember, name, request.role)
    }


}

