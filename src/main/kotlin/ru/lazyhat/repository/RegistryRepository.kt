package ru.lazyhat.repository

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import ru.lazyhat.db.services.LessonsService
import ru.lazyhat.db.services.RegistryService
import ru.lazyhat.db.services.StudentsService
import ru.lazyhat.models.*

interface RegistryRepository {
    suspend fun writeToRegistry(registryCreate: RegistryRecordCreate): Boolean
    suspend fun writeToRegistryWithStudent(registryRecordCreateStudent: RegistryRecordCreateStudent): Boolean
    suspend fun getAttendanceByLesson(lessonId: UInt): LessonAttendance
    suspend fun upsertListRecords(update: RegistryRecordUpdate): Boolean
}

class RegistryRepositoryImpl(
    val registryService: RegistryService,
    val lessonsService: LessonsService,
    val studentsService: StudentsService
) :
    RegistryRepository {
    override suspend fun writeToRegistry(registryCreate: RegistryRecordCreate): Boolean =
        registryService.create(registryCreate)

    override suspend fun writeToRegistryWithStudent(registryRecordCreateStudent: RegistryRecordCreateStudent): Boolean =
        registryService.create(
            RegistryRecordCreate(
                registryRecordCreateStudent.lessonId,
                registryRecordCreateStudent.student,
                LocalDateTime.now().date,
                AttendanceStatus.Attended
            )
        )

    override suspend fun upsertListRecords(update: RegistryRecordUpdate): Boolean = registryService.upsertOrDelete(update)

    override suspend fun getAttendanceByLesson(lessonId: UInt): LessonAttendance {
        val lesson = lessonsService.findById(lessonId)
        val listDates = lesson?.let { lessonEntry ->
            val plus = (lessonEntry.dayOfWeek.ordinal - lessonEntry.startDate.dayOfWeek.ordinal).let {
                if (it < 0)
                    it+7
                else
                    it
            }
            val startDateDOW = lessonEntry.startDate.plus(plus, DateTimeUnit.DAY)
            List(size = lessonEntry.durationWeeks.toInt()) {
                startDateDOW.plus(it * 7, DateTimeUnit.DAY)
            }
        } ?: listOf()
        val groups = lesson?.groups?.map { it to studentsService.findByGroup(it) }
        val studentAttendances = registryService.findByLesson(lessonId).groupBy { it.student }
        val la = LessonAttendance(lessonId, groups?.map {
            LessonAttendance.GroupAttendance(
                it.first, it.second.map {
                    LessonAttendance.GroupAttendance.StudentAttendance(
                        it,
                        studentAttendances[it.username].let {
                            val map = it?.associate { it.date to it.status }.orEmpty().toMutableMap()
                            listDates.forEach { map.getOrPut(it) { AttendanceStatus.Missing } }
                            map
                        }.toSortedMap()
                    )
                }
            )
        } ?: listOf())
        return la
    }
}