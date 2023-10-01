package ru.lazyhat.models

import io.ktor.server.auth.*
import kotlinx.datetime.LocalDateTime

sealed interface UserPrincipal : Principal {
    val username: String
    val expires_at: LocalDateTime

    data class StudentPrincipal(
        override val username: String,
        override val expires_at: LocalDateTime
    ) : UserPrincipal

    data class TeacherPrincipal(
        override val username: String,
        override val expires_at: LocalDateTime
    ) : UserPrincipal
}

