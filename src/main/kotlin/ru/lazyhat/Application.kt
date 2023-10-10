package ru.lazyhat

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import ru.lazyhat.db.configureMainModule
import ru.lazyhat.plugins.configureAuth
import ru.lazyhat.plugins.configureAuthModule
import ru.lazyhat.plugins.configureParsingData
import ru.lazyhat.routing.adminRouting
import ru.lazyhat.routing.guestRouting
import ru.lazyhat.routing.studentRouting
import ru.lazyhat.routing.teacherRouting

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    install(ContentNegotiation) { json() }
    install(Koin) {
        slf4jLogger()
        modules(
            configureMainModule(),
            configureAuthModule()
        )
    }
    configureAuth()
    configureParsingData()
    routing {
        guestRouting()
        studentRouting()
        teacherRouting()
        adminRouting()
    }
}
