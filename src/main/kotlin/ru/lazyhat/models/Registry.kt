package ru.lazyhat.models

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import java.util.*



data class RegistryRecord(
    val id: UUID,
    val lessonId: UInt,
    val student: String,
    val createdAt: LocalDateTime
)

data class RegistryRecordCreate(
    val lessonId: UInt,
    val student: String,
    val createdAt: LocalDateTime
)

enum class AttendanceStatus {
    Attended,
    Missing
}
data class LessonAttendance(
    val lessonId: UInt,
    val students: List<StudentAttendance>
) {
    data class StudentAttendance(
        val student: Student,
        val attendance: Map<LocalDate, AttendanceStatus>
    )
}