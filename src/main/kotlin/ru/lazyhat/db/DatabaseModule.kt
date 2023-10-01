package ru.lazyhat.db

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.Module
import org.koin.dsl.module
import ru.lazyhat.db.services.*
import ru.lazyhat.repository.LessonsRepository
import ru.lazyhat.repository.LessonsRepositoryImpl
import ru.lazyhat.repository.UserRepository
import ru.lazyhat.repository.UserRepositoryImpl

fun Application.configureDatabaseModule(): Module {
    val driverClassName = environment.config.property("storage.driverClassName").getString()
    val jdbcUrl = environment.config.property("storage.jdbcURL").getString()
    val dbUser = environment.config.property("storage.user").getString()
    val dbPass = environment.config.property("storage.pass").getString()
    val database = Database.connect(
        url = jdbcUrl,
        driver = driverClassName,
        //  user = dbUser,
        // password = dbPass
    )
    return module {
        single<StudentsService> { StudentsServiceImpl(database) }
        single<LessonsService> { LessonsServiceImpl(database) }
        single<TeachersService> { TeachersServiceImpl(database) }
        single<LessonTokensService> { LessonTokensServiceImpl(database) }
        single<GroupsService> { GroupsServiceImpl(database) }
        single<UserRepository> { UserRepositoryImpl(get(), get(), get()) }
        single<LessonsRepository> { LessonsRepositoryImpl(get()) }
    }
}