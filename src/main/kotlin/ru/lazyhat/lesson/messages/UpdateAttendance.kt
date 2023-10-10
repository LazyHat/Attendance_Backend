package ru.lazyhat.lesson.messages

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import ru.lazyhat.models.AttendanceStatus

@Serializable
data class UpdateAttendance(
    val student: String,
    val date: LocalDate,
    val newStatus: AttendanceStatus
)
