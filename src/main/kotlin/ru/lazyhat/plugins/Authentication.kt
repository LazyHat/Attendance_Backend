package ru.lazyhat.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import ru.lazyhat.models.Access

fun Application.configureAuth() {
    val jwtAuth by inject<JWTAuth>()
    install(Authentication) {
        jwt("user") {
            verifier(jwtAuth.buildVerifier())
            validate { credential ->
                with(jwtAuth) {
                    credential.toUserPrincipal()
                }
            }
            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
        jwt("student") {
            verifier(jwtAuth.buildVerifier())
            validate { credential ->
                with(jwtAuth) {
                    credential.toUserPrincipal()
                }?.takeIf { it.access == Access.Student }
            }
            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
        jwt("teacher") {
            verifier(jwtAuth.buildVerifier())
            validate { credential ->
                with(jwtAuth) {
                    credential.toUserPrincipal()
                }?.takeIf { it.access == Access.Teacher }
            }
            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }
}

fun Application.configureAuthModule(): Module = module {
    single {
        JWTAuth(environment)
    }
}