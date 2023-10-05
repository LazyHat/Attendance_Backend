package ru.lazyhat.models

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class Lesson(
    val id: UInt,
    val teacher: String,
    val title: String,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val durationHours: UInt,
    val startDate: LocalDate,
    val durationWeeks: UInt,
    val groups: Set<String>
)

@Serializable
data class LessonUpdate(
    val teacher: String,
    val title: String,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val durationHours: UInt,
    val startDate: LocalDate,
    val durationWeeks: UInt,
    val groups: Set<String>
)

@Serializable
data class LessonCreate(
    val title: String,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val durationHours: UInt,
    val startDate: LocalDate,
    val durationWeeks: UInt,
    val groups: Set<String>
)