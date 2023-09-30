package ru.lazyhat.db

import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.Module
import org.koin.dsl.module
import ru.lazyhat.db.services.*

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
        single { StudentsService(database) }
        single { QrCodeTokensService(database) }
        single { LessonsService(database) }
        single { ApiTokensService(database) }
    }
}

fun initDatabase(studentsService: StudentsService) {
    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {
        if (studentsService.isEmpty()) {
            studentsService.create(StudentCreateForm("lazyhat", "pass", "lazyfullname"))
        }
    }
}