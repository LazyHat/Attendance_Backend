package ru.lazyhat.repository

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import ru.lazyhat.db.services.LessonsService
import ru.lazyhat.db.services.RegistryService
import ru.lazyhat.db.services.StudentsService
import ru.lazyhat.models.AttendanceStatus
import ru.lazyhat.models.LessonAttendance
import ru.lazyhat.models.RegistryRecordCreate

interface RegistryRepository {
    suspend fun writeToRegistry(registryCreate: RegistryRecordCreate): Boolean
    suspend fun getAttendanceByLesson(lessonId: UInt): LessonAttendance
}

class RegistryRepositoryImpl(
    val registryService: RegistryService,
    val lessonsService: LessonsService,
    val studentsService: StudentsService
) :
    RegistryRepository {
    override suspend fun writeToRegistry(registryCreate: RegistryRecordCreate): Boolean =
        registryService.create(registryCreate)

    override suspend fun getAttendanceByLesson(lessonId: UInt): LessonAttendance {
        val listDates = lessonsService.findById(lessonId)?.let { lesson ->
            val range = (lesson.startDate.dayOfWeek.ordinal - lesson.dayOfWeek.ordinal).let {
                if (it < 0)
                    it + 7
                else
                    it
            }
            val startDate = lesson.startDate.plus(range, DateTimeUnit.DAY)
            List(size = lesson.durationWeeks.toInt()) {
                startDate.plus(it * 7, DateTimeUnit.DAY)
            }
        }

        return registryService.findByLesson(lessonId).let { list ->
            val students = list.groupBy { it.student }
            LessonAttendance(lessonId, students.map { entry ->
                val statuses =
                    entry.value.groupBy { it.createdAt.date }.mapValues { AttendanceStatus.Attended }.toMutableMap()
                listDates?.forEach {
                    if (!statuses.contains(it))
                        statuses[it] = AttendanceStatus.Missing
                }
                studentsService.findByUsername(entry.key)?.let { student ->
                    LessonAttendance.StudentAttendance(
                        student,
                        statuses.toMap()
                    )
                }
            }.filterNotNull())
        }
    }
}