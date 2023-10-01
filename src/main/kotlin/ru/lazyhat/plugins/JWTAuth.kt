package ru.lazyhat.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import ru.lazyhat.models.Access
import kotlin.time.Duration.Companion.minutes

@Serializable
data class UserPrincipal(val username: String, val access: Access, val expires_at: LocalDateTime) : Principal

class JWTAuth(environment: ApplicationEnvironment) {
    val secret = environment.config.property("jwt.secret").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    //val realm = environment.config.property("jwt.realm").getString()
    val validFor = 3.minutes
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
        val login = it.claims[Keys.username]?.asString()
        val access = it.claims[Keys.access]?.asString()?.let { Access.valueOf(it) }
        val expiresAt = it.expiresAtAsInstant?.toKotlinInstant()?.toLocalDateTime(TimeZone.currentSystemDefault())
        return if (login != null && access != null && expiresAt != null) {
            UserPrincipal(login, access, expiresAt)
        } else null
    }

    fun generateToken(username: String, access: Access): String = JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withPayload(createPayload(username, access))
        .withExpiresAt(Clock.System.now().plus(validFor).toJavaInstant())
        .sign(algorithm)

    fun buildVerifier(): JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaimPresence(Keys.username)
        .withClaimPresence(Keys.access)
        .build()
}