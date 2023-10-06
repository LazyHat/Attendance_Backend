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
            val range = (lessonEntry.dayOfWeek.ordinal - lessonEntry.startDate.dayOfWeek.ordinal).let {
                if (it < 0)
                    it + 6
                else
                    it
            }
            val startDate = lessonEntry.startDate.plus(range, DateTimeUnit.DAY)
            List(size = lessonEntry.durationWeeks.toInt()) {
                startDate.plus(it * 7, DateTimeUnit.DAY)
            }
        } ?: listOf()
        val groups = lesson?.groups?.map { it to studentsService.findByGroup(it) }
        val lessonAttendance = registryService.findByLesson(lessonId)
        val studentAttendances = lessonAttendance.groupBy { it.student }
        val la = LessonAttendance(lessonId, groups?.map {
            LessonAttendance.GroupAttendance(
                it.first, it.second.map {
                    LessonAttendance.GroupAttendance.StudentAttendance(
                        it,
                        studentAttendances[it.username].let {
                            val dates = listDates.associateWith { AttendanceStatus.Missing }.toMutableMap()
                            it?.forEach {
                                if(dates.contains(it.createdAt.date))
                                    dates[it.createdAt.date] = AttendanceStatus.Attended
                            }
                            dates.toMap()
                        }.toSortedMap()
                    )
                }
            )
        } ?: listOf())
        return la
    }
}