package ru.lazyhat.repository

import kotlinx.datetime.DateTimeUnit
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

    override suspend fun writeToRegistryWithStudent(registryRecordCreateStudent: RegistryRecordCreateStudent): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun upsertListRecords(update: RegistryRecordUpdate): Boolean {
        var count = 0
        update.recordsToUpdate.forEach {
            registryService.updateOrDelete(RegistryRecordCreate(it.lessonId, it.student, it.date, update.newStatus))
                .let {
                    if (it)
                        count++
                }
        }
        return count == update.recordsToUpdate.count()
    }

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