package ru.lazyhat.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import ru.lazyhat.models.RegistryRecordCreate
import ru.lazyhat.models.UserPrincipal
import ru.lazyhat.repository.LessonsRepository
import ru.lazyhat.repository.RegistryRepository
import ru.lazyhat.repository.UsersRepository
import java.util.*

fun Route.studentRouting() {
    val usersRepository by inject<UsersRepository>()
    val lessonsRepository by inject<LessonsRepository>()
    val registryRepository by inject<RegistryRepository>()
    authenticate("student") {
        route("student") {
            get("info") {
                val principal = call.principal<UserPrincipal.StudentPrincipal>()!!
                usersRepository.findStudentByUsername(principal.username)?.let {
                    call.respond(it)
                } ?: call.respond(HttpStatusCode.NotFound)
            }
            get("register") {
                call.request.queryParameters["token"]?.let { UUID.fromString(it) }?.let { token ->
                    val principal = call.principal<UserPrincipal.StudentPrincipal>()!!
                    lessonsRepository.getTokenInfo(token)?.let { lessonToken ->
                        registryRepository.writeToRegistry(RegistryRecordCreate(
                            lessonToken.lessonId,
                            principal.username
                        ))
                    } ?: call.respond(HttpStatusCode.NotFound)
                } ?: call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}