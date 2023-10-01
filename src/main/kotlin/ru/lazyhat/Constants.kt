package ru.lazyhat

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

object Constants {
    object Length {
        val username = 50
        val password = 32
        val group = 10
        val fullname = 100
        val title = 100
        val groupsList = 20
    }
    object TokensLives {
        val jwt = 3.days
        val lesson = 3.minutes
    }
}