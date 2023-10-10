package ru.lazyhat.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import ru.lazyhat.lesson.AlreadyExistsException
import ru.lazyhat.lesson.AttendanceController
import ru.lazyhat.models.LessonCreate
import ru.lazyhat.models.RegistryRecordUpdate
import ru.lazyhat.models.UserPrincipal
import ru.lazyhat.models.toLessonUpdate
import ru.lazyhat.repository.LessonsRepository
import ru.lazyhat.repository.UsersRepository

fun Route.teacherRouting() {
    val usersRepository by inject<UsersRepository>()
    val lessonsRepository by inject<LessonsRepository>()
    val attendanceController by inject<AttendanceController>()
    authenticate("teacher") {
        route("teacher") {
            get("info") {
                val principal = call.principal<UserPrincipal.TeacherPrincipal>()!!
                usersRepository.findTeacherByUsername(principal.username)?.let {
                    call.respond(it)
                } ?: call.respond(HttpStatusCode.NotFound)
            }
            get("students") {
                call.request.queryParameters["lesson"]?.toUIntOrNull()?.let { lessonId ->
                    lessonsRepository.getLessonById(lessonId)?.let {
                        call.respond(it.groups.associateWith { usersRepository.findStudentsByGroup(it) })
                    } ?: call.respond(HttpStatusCode.NotFound)
                } ?: call.respond(HttpStatusCode.BadRequest)
            }
            route("lessons") {
                get {
                    val principal = call.principal<UserPrincipal.TeacherPrincipal>()!!
                    lessonsRepository.getLessonsByUsername(principal.username).let {
                        call.respond(it)
                    }
                }
                post {
                    val principal = call.principal<UserPrincipal.TeacherPrincipal>()!!
                    val lesson: LessonCreate = call.receive()
                    lessonsRepository.createLesson(
                        lesson.toLessonUpdate(principal.username)
                    ).let {
                        if (it)
                            call.respond(HttpStatusCode.OK)
                        else
                            call.respond(HttpStatusCode.InternalServerError)
                    }
                }
                route("{id}") {
                    get {
                        call.parameters["id"]?.toUIntOrNull()?.let {
                            lessonsRepository.getLessonById(it)?.let {
                                call.respond(it)
                            } ?: call.respond(HttpStatusCode.NotFound)
                        } ?: call.respond(HttpStatusCode.BadRequest)
                    }
                    get("token") {
                        call.parameters["id"]?.toUIntOrNull()?.let { id ->
                            lessonsRepository.getLessonById(id)?.let {
                                lessonsRepository.createToken(id).let {
                                    call.respond(it)
                                }
                            } //Bad request (lower call already responds it)
                        } ?: call.respond(HttpStatusCode.BadRequest)
                    }
                    route("attendance") {
                        get {
                            call.parameters["id"]?.toUIntOrNull()?.let { id ->
                                call.respond(attendanceController.getAllAttendacnce(id))
                            } ?: call.respond(HttpStatusCode.BadRequest)
                        }
                        patch {
                            call.parameters["id"]?.toUIntOrNull()?.let { id ->
                                call.respond(attendanceController.updateAttendance(call.receive()))
                            } ?: call.respond(HttpStatusCode.BadRequest)
                        }
                        webSocket {
                            val principal = call.principal<UserPrincipal.TeacherPrincipal>()
                            if (principal == null) {
                                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No authorized"))
                                return@webSocket
                            }
                            try {
                                attendanceController.onJoin(
                                    principal.username,
                                    this
                                )
                                incoming.consumeEach { frame ->
                                    if (frame is Frame.Text) {
                                        val update = Json.decodeFromString<RegistryRecordUpdate>(frame.readText())
                                        attendanceController.updateAttendance(update)
                                    }
                                }
                            } catch (e: AlreadyExistsException) {
                                call.respond(HttpStatusCode.Conflict)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                attendanceController.tryDisconnect(principal.username)
                            }
                        }
                    }
                }
            }
        }
    }
}