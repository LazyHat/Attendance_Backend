package ru.lazyhat.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class LessonToken(
    val id: String,
    val lessonId: UInt,
    val expires: LocalDateTime
)

@Serializable
data class UserToken(
    val username: String,
    val access: Access,
    val expiresAt: LocalDateTime
)

@Serializable
data class Credentials(val username: String, val password: String)
