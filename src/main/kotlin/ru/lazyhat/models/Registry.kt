package ru.lazyhat.models

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable


@Serializable
data class RegistryRecord(
    val id: ULong,
    val lessonId: UInt,
    val student: String,
    val date: LocalDate,
    val status: AttendanceStatus
)

@Serializable
data class RegistryRecordCreate(
    val lessonId: UInt,
    val student: String,
    val date: LocalDate,
    val attendanceStatus: AttendanceStatus
)

@Serializable
data class RegistryRecordUpdate(
    val lessonId: UInt,
    val recordsToUpdate: List<Parameters>,
    val newStatus: AttendanceStatus
) {
    @Serializable
    data class Parameters(
        val student: String,
        val date: LocalDate
    )
}

@Serializable
data class RegistryRecordCreateStudent(
    val lessonId: UInt,
    val student: String,
)

@Serializable
enum class AttendanceStatus {
    Attended,
    Missing,
    ValidReason,
    Disease
}

@Serializable
data class LessonAttendance(
    val lessonId: UInt,
    val groups: List<GroupAttendance>
) {
    @Serializable
    data class GroupAttendance(
        val group: String,
        val attendance: List<StudentAttendance>
    ) {
        @Serializable
        data class StudentAttendance(
            val student: Student,
            val attendance: Map<LocalDate, AttendanceStatus>
        )
    }
}