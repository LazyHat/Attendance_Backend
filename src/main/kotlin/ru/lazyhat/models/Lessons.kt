package ru.lazyhat.models

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class Lesson(
    val id: UInt,
    val username: String,
    val title: String,
    val dayOfWeek: DayOfWeek,
    val start: LocalTime,
    val duration: Duration,
    val groupsList: Set<String>
)

@Serializable
data class LessonUpdate(
    val username: String,
    val title: String,
    val dayOfWeek: DayOfWeek,
    val start: LocalTime,
    val duration: Duration,
    val groupsList: Set<String>
)

@Serializable
data class LessonCreate(
    val title: String,
    val dayOfWeek: DayOfWeek,
    val start: LocalTime,
    val duration: Duration,
    val groupsList: Set<String>
)


fun LessonCreate.toLessonUpdate(username: String) = LessonUpdate(
    username, title, dayOfWeek, start, duration, groupsList
)