package ru.lazyhat.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import ru.lazyhat.models.Access
import ru.lazyhat.models.StudentCreate
import ru.lazyhat.models.Teacher
import ru.lazyhat.plugins.UserPrincipal
import ru.lazyhat.repository.UserRepository

fun Application.configureRouting() {
    val userRepository by inject<UserRepository>()
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
                    if (it)
                        call.respond(HttpStatusCode.Created)
                    else
                        call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
        route("teacher") {
            post("register") {
                val form: Teacher = call.receive()
                userRepository
            }
        }
        authenticate("student") {
            route("student") {
                get("info") {
                    call.principal<UserPrincipal>()?.let {
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
                    call.respond("success")
                }
            }
        }
        authenticate("user") {
            get("/token-info") {
                call.principal<UserPrincipal>()?.let {
                    call.respond(it)
                } ?: call.respond("principal null")
            }
        }
    }
}
