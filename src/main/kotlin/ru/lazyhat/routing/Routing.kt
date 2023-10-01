package ru.lazyhat.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import ru.lazyhat.models.*
import ru.lazyhat.repository.LessonsRepository
import ru.lazyhat.repository.UsersRepository

fun Application.configureRouting() {
    val userRepository by inject<UsersRepository>()
    val lessonsRepository by inject<LessonsRepository>()
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        route("student") {
            get("login") {
                val username = call.request.queryParameters["username"]
                val password = call.request.queryParameters["password"]
                if (username == null || password == null) {
                    call.respond(HttpStatusCode.BadRequest, "invalid login")
                } else {
                    val token = userRepository.logIn(username, password, Access.Student)
                    if (token != null) {
                        call.respond(token)
                    } else {
                        call.respond(HttpStatusCode.Unauthorized)
                    }
                }
            }
            post("register") {
                val form: StudentCreate = call.receive()
                userRepository.registerStudent(form).let {
                    call.respond(if (it) HttpStatusCode.Created else HttpStatusCode.BadRequest)
                }
            }
        }
        route("teacher") {
            post("register") {
                val form: Teacher = call.receive()
                userRepository.registerTeacher(form).let {
                    call.respond(if (it) HttpStatusCode.Created else HttpStatusCode.BadRequest)
                }
            }
            get("login") {
                val username = call.request.queryParameters["username"]
                val password = call.request.queryParameters["password"]
                if (username == null || password == null) {
                    call.respond(HttpStatusCode.BadRequest, "invalid login")
                } else {
                    val token = userRepository.logIn(username, password, Access.Teacher)
                    if (token != null) {
                        call.respond(token)
                    } else {
                        call.respond(HttpStatusCode.Unauthorized)
                    }
                }
            }
        }
        authenticate("student") {
            route("student") {
                get("info") {
                    call.principal<UserPrincipal.StudentPrincipal>()?.let {
                        userRepository.findStudentByUsername(it.username)?.let {
                            call.respond(it)
                        }
                    } ?: call.respond(HttpStatusCode.Unauthorized)
                }
            }
        }
        authenticate("teacher") {
            route("teacher") {
                get("info") {
                    call.principal<UserPrincipal.TeacherPrincipal>()?.let {
                        userRepository.findTeacherByUsername(it.username)?.let {
                            call.respond(it)
                        }
                    } ?: call.respond(HttpStatusCode.BadRequest)
                }
                get("students") {
                    call.request.queryParameters["lesson"]?.toUIntOrNull()?.let { lessonId ->
                        lessonsRepository.getLessonById(lessonId)?.let {
                            call.respond(it.groupsList.associateWith { userRepository.findStudentsByGroup(it) })
                        }
                    } ?: call.respond(HttpStatusCode.BadRequest)
                }
                route("lessons") {
                    get {
                        call.principal<UserPrincipal.TeacherPrincipal>()?.let {
                            lessonsRepository.getLessonsByUsername(it.username).let {
                                call.respond(it)
                            }
                        } ?: call.respond(HttpStatusCode.BadRequest)
                    }
                    post {
                        val principal = call.principal<UserPrincipal.TeacherPrincipal>()
                        val lesson: LessonCreate = call.receive()
                        principal?.let {
                            lessonsRepository.createLesson(
                                LessonUpdate(
                                    principal.username,
                                    lesson.title,
                                    lesson.start,
                                    lesson.end,
                                    lesson.groupsList
                                )
                            ).takeIf { it }?.let {
                                call.respond(HttpStatusCode.OK)
                            }
                        } ?: call.respond(HttpStatusCode.BadRequest)
                    }
                    get("{id}") {
                        call.parameters["id"]?.toUIntOrNull()?.let {
                            lessonsRepository.getLessonById(it)?.let {
                                call.respond(it)
                            }
                        } ?: call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
        }
        authenticate("user") {
            get("/token-info") {
                call.principal<UserPrincipal>()?.let {
                    call.respond(
                        when (it) {
                            is UserPrincipal.TeacherPrincipal -> UserToken(it.username, Access.Teacher, it.expires_at)
                            is UserPrincipal.StudentPrincipal -> UserToken(it.username, Access.Student, it.expires_at)
                        }
                    )
                } ?: call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}
