package ru.lazyhat.db

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.Module
import org.koin.dsl.module
import ru.lazyhat.db.services.*
import ru.lazyhat.lesson.AttendanceController
import ru.lazyhat.models.Credentials
import ru.lazyhat.repository.*

fun Application.configureMainModule(): Module {
    val driverClassName = environment.config.property("storage.driverClassName").getString()
    val jdbcUrl = environment.config.property("storage.jdbcURL").getString()
    val dbUser = environment.config.property("storage.user").getString()
    val dbPass = environment.config.property("storage.pass").getString()
    val superUsername = environment.config.property("super.username").getString()
    val superPassword = environment.config.property("super.password").getString()
    val database = Database.connect(
        url = jdbcUrl,
        driver = driverClassName,
        //  user = dbUser,
        // password = dbPass
    )
    return module {
        single<StudentsService> { StudentsServiceImpl(database) }
        single<TeachersService> { TeachersServiceImpl(database) }
        single<LessonsService> { LessonsServiceImpl(database) }
        single<LessonTokensService> { LessonTokensServiceImpl(database) }
        single<RegistryService> { RegistryServiceImpl(database) }
        single<UsersRepository> { UsersRepositoryImpl(get(), get(), get()) }
        single<LessonsRepository> { LessonsRepositoryImpl(get(), get()) }
        single<RegistryRepository> { RegistryRepositoryImpl(get(), get(), get()) }
        single<AttendanceController> { AttendanceController(get()) }
        single<AdminRepository> {
            AdminRepositoryImpl(
                get(),
                get(),
                get(),
                get(),
                get(),
                Credentials(superUsername, superPassword)
            )
        }
    }
}