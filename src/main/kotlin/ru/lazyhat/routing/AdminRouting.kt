package ru.lazyhat.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import ru.lazyhat.models.Credentials
import ru.lazyhat.repository.AdminRepository

fun Route.adminRouting() {
    val adminRepository by inject<AdminRepository>()
    get("admin/login") {
        val username = call.request.queryParameters["username"]
        val password = call.request.queryParameters["password"]
        if (username != null && password != null) {
            val credentials = Credentials(username, password)
            adminRepository.validateSuperUser(credentials).let {
                if (it)
                    call.respond(adminRepository.createUserToken(credentials))
                else
                    call.respond(HttpStatusCode.Forbidden)
            }
        } else call.respond(HttpStatusCode.BadRequest)
    }

    authenticate("admin") {
        route("admin") {
            route("lessons") {
                get {
                    call.respond(adminRepository.getAllLessons())
                }
                post {
                    adminRepository.createLesson(call.receive()).let {
                        call.respond(if (it) HttpStatusCode.Created else HttpStatusCode.BadRequest)
                    }
                }
                route("{id}") {
                    get {
                        call.parameters["id"]?.toUIntOrNull()?.let {
                            adminRepository.getLessonById(it)?.let {
                                call.respond(it)
                            } ?: call.respond(HttpStatusCode.NoContent)
                        } ?: call.respond(HttpStatusCode.BadRequest)
                    }
                    get("attendance") {
                        call.parameters["id"]?.toUIntOrNull()?.let {
                            call.respond(adminRepository.getLessonAttendance(it))
                        } ?: call.respond(HttpStatusCode.BadRequest)
                    }
//                    patch {
//                        call.receive<LessonUpdate>().let {
//                            adminRepository.
//                        }
//                    }
                }
            }
            route("students") {
                get {
                    call.respond(adminRepository.getAllStudents())
                }
                post {
                    adminRepository.createStudent(call.receive()).let {
                        call.respond(if (it) HttpStatusCode.Created else HttpStatusCode.BadRequest)
                    }
                }
                route("{username}") {
                    delete {
                        call.parameters["username"]?.let { username ->
                            adminRepository.deleteStudent(username).let {
                                call.respond(if (it) HttpStatusCode.OK else HttpStatusCode.NotFound)
                            }
                        } ?: call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
            route("teachers") {
                get {
                    call.respond(adminRepository.getAllTeachers())
                }
                route("username") {
                    delete {
                        call.parameters["username"]?.let { username ->
                            adminRepository.deleteTeacher(username).let {
                                call.respond(if (it) HttpStatusCode.OK else HttpStatusCode.NotFound)
                            }
                        } ?: call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
        }
    }
}