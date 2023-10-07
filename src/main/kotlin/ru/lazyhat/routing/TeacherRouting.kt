package ru.lazyhat.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import ru.lazyhat.models.LessonCreate
import ru.lazyhat.models.UserPrincipal
import ru.lazyhat.models.toLessonUpdate
import ru.lazyhat.repository.LessonsRepository
import ru.lazyhat.repository.RegistryRepository
import ru.lazyhat.repository.UsersRepository

fun Route.teacherRouting() {
    val usersRepository by inject<UsersRepository>()
    val lessonsRepository by inject<LessonsRepository>()
    val registryRepository by inject<RegistryRepository>()
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
                                call.respond(registryRepository.getAttendanceByLesson(id))
                            } ?: call.respond(HttpStatusCode.BadRequest)
                        }
                        patch {
                            call.parameters["id"]
                        }
                    }
                }
            }
        }
    }
}