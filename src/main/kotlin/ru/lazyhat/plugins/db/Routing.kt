package ru.lazyhat.plugins.db

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

data class Student(val fullName: String)

fun Route.databaseRouting(userService: UserService, tokensService: TokensService, lessonsService: LessonsService) {
    // Create user
    post("/users") {
        val user = call.receive<UserNew>()
        val id = userService.create(user)
        call.respond(HttpStatusCode.Created, id)
    }
    // Read user
    get("/users/{id}") {
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid ID")
        val user = userService.read(id)
        if (user != null) {
            call.respond(HttpStatusCode.OK, user)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }
    // Update user
    put("/users/{id}") {
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid ID")
        val user = call.receive<UserResult>()
        userService.update(id, user)
        call.respond(HttpStatusCode.OK)
    }
    // Delete user
    delete("/users/{id}") {
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid ID")
        userService.delete(id)
        call.respond(HttpStatusCode.OK)
    }
    //get students on lesson
    get("/lessons/{id}/students") {
        val lessonId = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")
        call.respond(userService.getRegisteredUsers(lessonId).map { Student(it.fullName) })
    }
    //create token
    get("/lessons/{id}/token") {
        val lessonId = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")
        call.respond(tokensService.create(lessonId))
    }
    //create lesson
    post("/lessons") {
        val lesson: Lesson = call.receive()
        lessonsService.create(lesson)
        call.respond(HttpStatusCode.OK)
    }
    //delete lesson
    delete("/lessons/{id}") {
        val lessonId = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")
        lessonsService.delete(lessonId)
    }
    //get lesson Info
    get("/lessons/{id}") {
        val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")
        val lesson = lessonsService.read(id)
        lesson?.let { call.respond(it) } ?: call.respond(HttpStatusCode.BadRequest)
    }
    get("/tokens/{id}") {
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Invalid ID")
        val token = tokensService.read(id)
        token?.let { call.respond(it) } ?: call.respond(HttpStatusCode.BadRequest)
    }
    //register student
    patch("/register/{token}") {
        val token = call.parameters["token"] ?: throw IllegalArgumentException("Invalid ID")
        val student = call.request.queryParameters["student"] ?: throw IllegalArgumentException("Invalid Student")
        val lessonId = tokensService.read(token)?.lessonId ?: throw IllegalArgumentException("Token does not exists")
        val lesson = lessonsService.read(lessonId)
        if (lesson != null) {
            userService.setLesson(student, lessonId)
            call.respond(HttpStatusCode.OK, lesson)
        } else call.respond(HttpStatusCode.BadRequest)
    }
}