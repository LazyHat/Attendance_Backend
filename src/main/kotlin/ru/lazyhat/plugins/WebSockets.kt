package ru.lazyhat.plugins

import io.ktor.server.application.*
import io.ktor.server.websocket.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

fun Application.configureWebSockets(){
    install(WebSockets){
        pingPeriod = 5.seconds.toJavaDuration()
        timeout = 30.seconds.toJavaDuration()
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}