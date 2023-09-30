package ru.lazyhat

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import ru.lazyhat.db.configureDatabaseModule
import ru.lazyhat.plugins.configureAuth
import ru.lazyhat.routing.configureRouting

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    install(ContentNegotiation) { json() }
    install(Koin){
        slf4jLogger()
        modules(configureDatabaseModule())
    }
    configureAuth()
    configureRouting()
}
