package ru.lazyhat.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import ru.lazyhat.models.Access
import ru.lazyhat.models.StudentCreate
import ru.lazyhat.models.Teacher
import ru.lazyhat.repository.UsersRepository

fun Route.guestRouting() {
    val userRepository by inject<UsersRepository>()
    get("/") {
        call.respondText("Hello World!")
    }
    route("student") {
        get("login") {
            val username = call.request.queryParameters["username"]
            val password = call.request.queryParameters["password"]
            if (username == null || password == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid parameters")
            } else {
                val token = userRepository.logIn(username, password, Access.Student)
                if (token != null) {
                    call.respond(token)
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
        post("register") {
            val form: StudentCreate = call.receive()
            userRepository.registerStudent(form).let {
                call.respond(if (it) HttpStatusCode.Created else HttpStatusCode.Conflict)
            }
        }
    }
    route("teacher") {
        post("register") {
            val form: Teacher = call.receive()
            userRepository.registerTeacher(form).let {
                call.respond(if (it) HttpStatusCode.Created else HttpStatusCode.Conflict)
            }
        }
        get("login") {
            val username = call.request.queryParameters["username"]
            val password = call.request.queryParameters["password"]
            if (username == null || password == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid parameters")
            } else {
                val token = userRepository.logIn(username, password, Access.Teacher)
                if (token != null) {
                    call.respond(token)
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }
}