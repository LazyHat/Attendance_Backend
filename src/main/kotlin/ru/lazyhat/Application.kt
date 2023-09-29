package ru.lazyhat

import io.ktor.server.application.*
import io.ktor.server.netty.*
import ru.lazyhat.plugins.*
import ru.lazyhat.plugins.db.configureDatabases

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    configureSerialization()
    configureDatabases()
    configureRouting()
}
