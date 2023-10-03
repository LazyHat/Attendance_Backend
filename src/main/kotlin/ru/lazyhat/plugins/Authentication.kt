package ru.lazyhat.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import ru.lazyhat.models.Credentials
import ru.lazyhat.models.UserPrincipal
import ru.lazyhat.repository.AdminRepository
import ru.lazyhat.repository.UsersRepository

fun Application.configureAuth() {
    val jwtAuth by inject<JWTAuth>()
    val usersRepository by inject<UsersRepository>()
    val adminRepository by inject<AdminRepository>()
    install(Authentication) {
        jwt("student") {
            verifier(jwtAuth.buildVerifier())
            validate { credential ->
                with(jwtAuth) {
                    credential.toUserPrincipal()
                }?.let { it as? UserPrincipal.StudentPrincipal }
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
                }?.let { it as? UserPrincipal.TeacherPrincipal }?.takeIf {
                    usersRepository.findTeacherByUsername(it.username)?.password == it.password
                }
            }
            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
        jwt("admin") {
            verifier(jwtAuth.buildVerifier())
            validate { credential ->
                with(jwtAuth) {
                    credential.toUserPrincipal()
                }?.let { it as? UserPrincipal.AdminPrincipal }?.takeIf {
                    adminRepository.validateSuperUser(Credentials(it.username, it.password))
                }
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