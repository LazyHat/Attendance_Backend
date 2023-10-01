package ru.lazyhat.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Lesson(
    val id: UInt,
    val username: String,
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val groupsList: Set<String>
)

@Serializable
data class LessonUpdate(
    val username: String,
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val groupsList: Set<String>
)

@Serializable
data class LessonCreate(
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val groupsList: Set<String>
)