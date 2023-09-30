package ru.lazyhat.routing

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlin.time.Duration.Companion.days

fun Application.configureRouting() {
    val secret = environment.config.property("jwt.secret").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val realm = environment.config.property("jwt.realm").getString()
    //Create API token (Log in user)
//    get("/student/login") {
//        val login = call.request.queryParameters["login"]
//        val password = call.request.queryParameters["password"]
//        if (login == null || password == null)
//            call.respond(HttpStatusCode.BadRequest, "invalid parameters")
//        else {
//            if (studentsService.authentificate(login, password)) {
//                val token = apiTokensService.create(login, ApiToken.Access.Student)
//                call.respond(HttpStatusCode.Created, token)
//            } else
//                call.respond(HttpStatusCode.BadRequest, "login or password are incorrect")
//        }
//    }
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        get("login") {
            val username = call.request.queryParameters["username"]
            if (username == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid login")
            } else {
                val token = JWT.create()
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .withClaim("username", username)
                    .withExpiresAt(Clock.System.now().plus(3.days).toJavaInstant())
                    .sign(Algorithm.HMAC256(secret))
                call.respond(token)
            }
        }
        authenticate {
            get("/token") {
                val principal = call.principal<JWTPrincipal>()?.let {
                    val userName = it.payload.getClaim("username").asString()
                    call.respond("username: $userName")
                } ?: call.respond("principal null")
            }
        }
    }
}
