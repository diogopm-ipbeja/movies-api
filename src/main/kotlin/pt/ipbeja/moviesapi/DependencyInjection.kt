package pt.ipbeja.moviesapi

import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import pt.ipbeja.moviesapi.entities.common.FileRepresentation
import pt.ipbeja.moviesapi.entities.genres.model.Genre
import pt.ipbeja.moviesapi.entities.genres.usecases.commands.*
import pt.ipbeja.moviesapi.entities.genres.usecases.queries.GetGenresQuery
import pt.ipbeja.moviesapi.entities.genres.usecases.queries.GetGenresQueryHandler
import pt.ipbeja.moviesapi.entities.movies.model.CastMember
import pt.ipbeja.moviesapi.entities.movies.model.MovieDetail
import pt.ipbeja.moviesapi.entities.movies.model.MovieSimple
import pt.ipbeja.moviesapi.entities.movies.model.RoleAssignment
import pt.ipbeja.moviesapi.entities.movies.usecases.commands.*
import pt.ipbeja.moviesapi.entities.movies.usecases.queries.*
import pt.ipbeja.moviesapi.entities.people.Person
import pt.ipbeja.moviesapi.entities.people.PersonDetail
import pt.ipbeja.moviesapi.entities.people.PersonSimple
import pt.ipbeja.moviesapi.entities.people.usecases.commands.*
import pt.ipbeja.moviesapi.entities.people.usecases.queries.GetPeopleQuery
import pt.ipbeja.moviesapi.entities.people.usecases.queries.GetPeopleQueryHandler
import pt.ipbeja.moviesapi.entities.people.usecases.queries.GetPersonQuery
import pt.ipbeja.moviesapi.entities.people.usecases.queries.GetPersonQueryHandler
import pt.ipbeja.moviesapi.entities.users.UserInfo
import pt.ipbeja.moviesapi.entities.users.usecases.commands.*
import pt.ipbeja.moviesapi.entities.users.usecases.queries.*
import pt.ipbeja.moviesapi.services.StorageService
import pt.ipbeja.moviesapi.utilities.RequestHandler
import pt.ipbeja.moviesapi.utilities.ValueOr
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists




fun Application.configureFrameworks(database: Database) {
    install(Koin) {
        slf4jLogger()




        val masterAdmin = environment.config.property("administration.user").getString()
        val path = Path("/opt/movies-api/")
        if (!path.exists()) path.createDirectories()

        modules(module {
            single { StorageService(path) }
            single { database }
            /*// genres
            factory<RequestHandler<CreateGenresCommand, List<Genre>>> { CreateGenresCommandHandler(get()) }
            // factory<RequestHandler<CreateGenresCommand, List<Genre>>> { CreateGenresCommandHandler(get()) }
            factory<RequestHandler<UpdateGenreCommand, Genre>> { UpdateGenreCommandHandler(get()) }
            factory<RequestHandler<DeleteGenresCommand, Int>> { DeleteGenresCommandHandler(get()) }
            factory<RequestHandler<GetGenresQuery, List<Genre>>> { GetGenresQueryHandler(get()) }

            // movies

            factory<RequestHandler<GetMovieQuery, ValueOr<MovieDetail>>> { GetMovieQueryHandler(get()) }
            factory<RequestHandler<GetMoviesQuery, List<MovieSimple>>> { GetMoviesQueryHandler(get()) }
            factory<RequestHandler<GetMovieRatingQuery, MovieRating?>> { GetMovieRatingQueryHandler(get()) }
            factory<RequestHandler<GetMoviePictureQuery, ValueOr<FileRepresentation>>> {
                GetMoviePictureQueryHandler(
                    get(),
                    get()
                )
            }


            factory<RequestHandler<CreateMovieCommand, MovieDetail>> { CreateMovieCommandHandler(get(), get()) }
            factory<RequestHandler<CreateMoviesCommand, List<MovieDetail>>> { CreateMoviesCommandHandler(get(), get()) }
            factory<RequestHandler<DeleteMovieCommand, Unit>> { DeleteMovieCommandHandler(get()) }

            factory<RequestHandler<DeleteMovieRatingCommand, ValueOr<Unit>>> { DeleteMovieRatingCommandHandler(get()) }
            factory<RequestHandler<RateMovieCommand, Unit>> { RateMovieCommandHandler(get()) }
            factory<RequestHandler<AddCastMembersCommand, List<RoleAssignment>>> { AddCastMembersCommandHandler(get()) }
            factory<RequestHandler<AddCastMembersCommand2, List<CastMember>>> { AddCastMembersCommandHandler2(get()) }
            factory<RequestHandler<AddMoviePicturesCommand, Unit>> { AddMoviePicturesCommandHandler(get(), get()) }
            factory<RequestHandler<RemoveMoviePicturesCommand, ValueOr<Int>>> {
                RemoveMoviePicturesCommandHandler(
                    get(),
                    get()
                )
            }
            factory<RequestHandler<SetMovieMainPictureCommand, Unit>> { SetMovieMainPictureCommandHandler(get()) }
            factory<RequestHandler<SetUserFavoriteCommand, Unit>> { SetUserFavoriteCommandHandler(get()) }
            factory<RequestHandler<UpdateMovieCommand, MovieSimple>> { UpdateMovieCommandHandler(get()) }
            factory<RequestHandler<RemoveCastMembersCommand, Int>> { RemoveCastMembersCommandHandler(get()) }
            factory<RequestHandler<UpdateCastMemberCommand, CastMember?>> { UpdateCastMemberCommandHandler(get()) }


            // people

            factory<RequestHandler<GetPeopleQuery, List<PersonSimple>>> { GetPeopleQueryHandler(get()) }
            factory<RequestHandler<GetPersonQuery, ValueOr<PersonDetail>>> { GetPersonQueryHandler(get()) }

            factory<RequestHandler<CreatePeopleCommand, List<Person>>> { CreatePeopleCommandHandler(get(), get()) }
            factory<RequestHandler<DeletePeopleCommand, Int>> { DeletePeopleCommandHandler(get()) }
            factory<RequestHandler<UpdatePersonCommand, Person>> { UpdatePersonCommandHandler(get()) }
            factory<RequestHandler<AddPicturesToPersonCommand, Person>> { AddPicturesToPersonCommandHandler(get(), get()) }
            factory<RequestHandler<RemovePicturesFromPersonCommand, Person>> {RemovePicturesFromPersonCommandHandler(get(), get()) }



            // users
            factory<RequestHandler<GetUserQuery, UserInfo?>> { GetUserQueryHandler(get()) }
            factory<RequestHandler<GetUsersQuery, List<UserInfo>>> { GetUsersQueryHandler(get()) }
            factory<RequestHandler<GetUserPictureQuery, FileRepresentation?>> { GetUserPictureQueryHandler(get(), get())}
            factory<RequestHandler<GetUserRatingsQuery, List<UserRating>?>> { GetUserRatingsQueryHandler(get()) }

            factory<RequestHandler<RegisterUserCommand, UserInfo>> { RegisterUserCommandHandler(get(), get()) }
            factory<RequestHandler<DeleteUserCommand, Unit>> { DeleteUserCommandHandler(get(), masterAdmin) }
            factory<RequestHandler<ChangeUserPasswordCommand, Unit>> { ChangeUserPasswordCommandHandler(get()) }
            factory<RequestHandler<SetUserPictureCommand, UserInfo>> { SetUserPictureCommandHandler(get(), get()) }


            // factory<RequestHandler<GetMovieByIdRequest, ValueOr<String>>> { GetMovieByIdRequestHandler(get()) }

*/

            factory { CreateGenresCommandHandler(get()) }
            factory { CreateGenresCommandHandler(get()) }
            factory { UpdateGenreCommandHandler(get()) }
            factory { DeleteGenresCommandHandler(get()) }
            factory { GetGenresQueryHandler(get()) }

            factory { GetMovieQueryHandler(get()) }
            factory { GetMoviesQueryHandler(get()) }
            factory { GetMovieRatingQueryHandler(get()) }
            factory { CreateMovieCommandHandler(get(), get()) }
            factory { CreateMoviesCommandHandler(get(), get()) }
            factory { DeleteMovieCommandHandler(get()) }
            factory { DeleteMovieRatingCommandHandler(get()) }
            factory { RateMovieCommandHandler(get()) }
            factory { AddCastMembersCommandHandler(get()) }
            factory { AddCastMembersCommandHandler2(get()) }
            factory { AddMoviePicturesCommandHandler(get(), get()) }
            factory { SetMovieMainPictureCommandHandler(get()) }
            factory { SetUserFavoriteCommandHandler(get()) }
            factory { UpdateMovieCommandHandler(get()) }
            factory { RemoveCastMembersCommandHandler(get()) }
            factory { UpdateCastMemberCommandHandler(get()) }

            factory { GetPeopleQueryHandler(get()) }
            factory { GetPersonQueryHandler(get()) }
            factory { CreatePeopleCommandHandler(get(), get()) }
            factory { DeletePeopleCommandHandler(get()) }
            factory { UpdatePersonCommandHandler(get()) }
            factory { AddPicturesToPersonCommandHandler(get(), get()) }

            factory { GetUserQueryHandler(get()) }
            factory { GetUsersQueryHandler(get()) }
            factory { GetUserPictureQueryHandler(get(), get())}
            factory { GetUserRatingsQueryHandler(get()) }
            factory { RegisterUserCommandHandler(get(), get()) }
            factory { DeleteUserCommandHandler(get(), masterAdmin) }
            factory { ChangeUserPasswordCommandHandler(get()) }
            factory { SetUserPictureCommandHandler(get(), get()) }


        })


    }

}
