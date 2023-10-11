package ru.lazyhat.lesson

import io.ktor.websocket.*
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.lazyhat.lesson.messages.UpdateAttendance
import ru.lazyhat.models.*
import ru.lazyhat.repository.LessonsRepository
import ru.lazyhat.repository.RegistryRepository
import java.util.concurrent.ConcurrentHashMap

class AttendanceController(
    private val registryRepository: RegistryRepository,
    private val lessonsRepository: LessonsRepository
) {
    private val teachers = ConcurrentHashMap<String, TeacherSocketSession>()
    fun onJoin(
        username: String,
        socket: WebSocketSession
    ) {
        if (teachers.containsKey(username)) {
            throw AlreadyExistsException()
        }
        teachers[username] = TeacherSocketSession(username, socket)
    }

    suspend fun registerStudentToLesson(record: RegistryRecordCreateStudent) {
        val date = LocalDateTime.now().date
        val lesson = lessonsRepository.getLessonById(record.lessonId)
        if (lesson != null)
            if (registryRepository.writeToRegistryWithStudent(record))
                teachers[lesson.teacher]?.socket?.send(
                    Frame.Text(
                        Json.encodeToString(
                            UpdateAttendance(
                                record.student,
                                date,
                                AttendanceStatus.Attended
                            )
                        )
                    )
                )
    }

    suspend fun updateAttendance(update: RegistryRecordUpdate) {
        val lesson = lessonsRepository.getLessonById(update.lessonId)
        if (lesson != null)
            if (registryRepository.upsertListRecords(update))
                teachers[lesson.teacher]?.let { teacher ->
                    update.recordsToUpdate.map { UpdateAttendance(it.student, it.date, update.newStatus) }.let {
                        teacher.socket.send(
                            Frame.Text(
                                Json.encodeToString(it)
                            )
                        )
                    }
                }
    }

    suspend fun getAllAttendacnce(lessonId: UInt): LessonAttendance = registryRepository.getAttendanceByLesson(lessonId)

    suspend fun tryDisconnect(username: String) {
        teachers[username]?.socket?.close()
        if (teachers.containsKey(username)) {
            teachers.remove(username)
        }
    }
}