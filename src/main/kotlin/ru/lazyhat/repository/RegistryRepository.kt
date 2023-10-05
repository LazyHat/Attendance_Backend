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
        val lesson = lessonsService.findById(lessonId)
        val listDates = lesson?.let { lessonEntry ->
            val range = (lessonEntry.startDate.dayOfWeek.ordinal - lessonEntry.dayOfWeek.ordinal).let {
                if (it < 0)
                    it + 7
                else
                    it
            }
            val startDate = lessonEntry.startDate.plus(range, DateTimeUnit.DAY)
            List(size = lessonEntry.durationWeeks.toInt()) {
                startDate.plus(it * 7, DateTimeUnit.DAY)
            }
        }
        val groups = lesson?.groups?.map { it to studentsService.findByGroup(it) }
        return LessonAttendance(lessonId, groups?.map {
            LessonAttendance.GroupAttendance(
                it.first, it.second.map {
                    LessonAttendance.GroupAttendance.StudentAttendance(
                        it,
                        registryService.findByStudent(it.username).filter { it.lessonId == lessonId }.let {
                            val statuses =
                                it.groupBy { it.createdAt.date }.mapValues { AttendanceStatus.Attended }.toMutableMap()
                            listDates?.forEach {
                                if (!statuses.contains(it))
                                    statuses[it] = AttendanceStatus.Missing
                            }
                            statuses.toMap()
                        }
                    )
                }
            )
        } ?: listOf())
    }
}