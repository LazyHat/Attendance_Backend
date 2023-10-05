package ru.lazyhat.models

import kotlinx.datetime.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

operator fun LocalTime.plus(duration: Duration): LocalTime =
    (this.toMillisecondOfDay() + duration.inWholeMilliseconds).let {
        LocalTime.fromMillisecondOfDay(it.toInt())
    }

operator fun LocalDateTime.minus(other: LocalDateTime): Duration =
    this.toInstant(TimeZone.currentSystemDefault()) - other.toInstant(TimeZone.currentSystemDefault())

operator fun LocalDateTime.plus(duration: Duration): LocalDateTime =
    this.toInstant(TimeZone.currentSystemDefault()).plus(duration).toLocalDateTime(TimeZone.currentSystemDefault())

fun LessonCreate.toLessonUpdate(username: String) = LessonUpdate(
    username, title, dayOfWeek, startTime, durationHours, startDate, durationWeeks, groups
)

fun LocalDateTime.Companion.now(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

fun Duration.roundTo(unit: DurationUnit) = this.toLong(unit).toDuration(unit)

fun RegistryRecord.toRegistryRecordCreate(): RegistryRecordCreate = RegistryRecordCreate(lessonId, student, createdAt)
fun RegistryRecordCreate.toRegistryRecord(id: ULong): RegistryRecord = RegistryRecord(id, lessonId, student, createdAt)