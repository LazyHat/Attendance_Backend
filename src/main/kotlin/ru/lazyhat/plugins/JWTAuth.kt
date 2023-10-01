package ru.lazyhat.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import kotlinx.datetime.*
import ru.lazyhat.Constants
import ru.lazyhat.models.Access
import ru.lazyhat.models.UserPrincipal

class JWTAuth(environment: ApplicationEnvironment) {
    val secret = environment.config.property("jwt.secret").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()

    //val realm = environment.config.property("jwt.realm").getString()
    val algorithm = Algorithm.HMAC256(secret)

    companion object {
        private object Keys {
            val username = "username"
            val access = "access"
        }
    }

    fun createPayload(username: String, access: Access): Map<String, String> =
        mapOf(Keys.username to username, Keys.access to access.name)

    fun JWTCredential.toUserPrincipal(): UserPrincipal? = this.payload.let {
        val username = it.claims[Keys.username]?.asString()
        val access = it.claims[Keys.access]?.asString()?.let { Access.valueOf(it) }
        val expiresAt = it.expiresAtAsInstant?.toKotlinInstant()?.toLocalDateTime(TimeZone.currentSystemDefault())
        return if (username != null && access != null && expiresAt != null) {
            when (access) {
                Access.Teacher -> UserPrincipal.TeacherPrincipal(username, expiresAt)
                Access.Student -> UserPrincipal.StudentPrincipal(username, expiresAt)
            }
        } else null
    }

    fun generateToken(username: String, access: Access): String = JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withPayload(createPayload(username, access))
        .withExpiresAt(Clock.System.now().plus(Constants.TokensLives.jwt).toJavaInstant())
        .sign(algorithm)

    fun buildVerifier(): JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaimPresence(Keys.username)
        .withClaimPresence(Keys.access)
        .build()
}