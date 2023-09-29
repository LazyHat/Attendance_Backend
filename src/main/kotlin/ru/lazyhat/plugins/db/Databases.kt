package ru.lazyhat.plugins.db

import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabases() {
    val driverClassName = environment.config.property("storage.driverClassName").getString()
    val jdbcUrl = environment.config.property("storage.jdbcURL").getString()
    val dbUser = environment.config.property("storage.user").getString()
    val dbPass = environment.config.property("storage.pass").getString()
    val database = Database.connect(
        url = jdbcUrl,
        driver = driverClassName,
        user = dbUser,
        password = dbPass
    )
    val userService = UserService(database)
    val tokensService = TokensService(database)
    val lessonsService = LessonsService(database)
    routing {
        databaseRouting(userService, tokensService, lessonsService)
    }
}
