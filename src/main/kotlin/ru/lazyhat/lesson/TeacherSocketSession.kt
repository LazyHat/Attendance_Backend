package ru.lazyhat.lesson

import io.ktor.websocket.*

data class TeacherSocketSession(
    val username: String,
    val socket: WebSocketSession
)
