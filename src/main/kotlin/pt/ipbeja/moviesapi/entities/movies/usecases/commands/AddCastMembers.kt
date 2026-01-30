package pt.ipbeja.moviesapi.entities.movies.usecases.commands

import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pt.ipbeja.moviesapi.CastMemberEntity
import pt.ipbeja.moviesapi.CastMembers
import pt.ipbeja.moviesapi.Movies
import pt.ipbeja.moviesapi.Persons
import pt.ipbeja.moviesapi.entities.movies.model.CastMember
import pt.ipbeja.moviesapi.entities.movies.model.RoleAssignment
import pt.ipbeja.moviesapi.utilities.Request
import pt.ipbeja.moviesapi.utilities.RequestHandler


data class AddCastMembersCommand(val movieId: Int, val castMembers : List<RoleAssignment>) : Request<List<RoleAssignment>>

class AddCastMembersCommandHandler(val db: Database) : RequestHandler<AddCastMembersCommand, List<RoleAssignment>> {

    override suspend fun handle(request: AddCastMembersCommand): List<RoleAssignment> = transaction(db) {

        val sanitized = request.castMembers.map { it.copy(role = it.role.trim()) }


        /*return@transaction sanitized.map { r ->
            CastMemberEntity.new {
                this.personId = EntityID(r.personId, Persons)
                this.movieId = EntityID(request.movieId, Movies)
            }
        }.map {
            CastMember(it.personId.value, it.person.name, it.role.value)
        }
        */


        CastMembers.batchInsert(sanitized) {
            this[CastMembers.movie] = EntityID(request.movieId, Movies)
            this[CastMembers.person] = EntityID(it.personId, Persons)
            this[CastMembers.role] = it.role
        }

        sanitized
    }

}

data class AddCastMembersCommand2(val movieId: Int, val castMembers : List<RoleAssignment>) : Request<List<CastMember>>

class AddCastMembersCommandHandler2(val db: Database) : RequestHandler<AddCastMembersCommand2, List<CastMember>> {

    override suspend fun handle(request: AddCastMembersCommand2): List<CastMember> = transaction(db) {

        val sanitized = request.castMembers.map { it.copy(role = it.role.trim()) }

        return@transaction sanitized.map { r ->
            val id = CompositeID {
                it[CastMembers.person] = request.movieId
                it[CastMembers.movie] = r.personId
            }
            CastMemberEntity.new(id) {
                this.role = r.role

            }
        }.map {
            CastMember(it.personId.value, it.person.name, it.role)
        }

    }

}



