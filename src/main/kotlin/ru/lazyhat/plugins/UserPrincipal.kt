package ru.lazyhat.plugins

import com.auth0.jwt.interfaces.Claim
import io.ktor.server.auth.*
import kotlinx.serialization.Serializable
import ru.lazyhat.db.services.Access

@Serializable
data class UserPrincipal(val username: String, val access: Access) : Principal {
    companion object {
        private object Keys {
            val username = "username"
            val access = "access"
        }

        fun fromPayload(claims: Map<String, Claim>): UserPrincipal? {
            val login = claims[Keys.username]?.asString()
            val access = claims[Keys.access]?.asString()?.let { Access.valueOf(it) }
            return if (login != null && access != null) {
                UserPrincipal(login, access)
            } else null
        }
    }

    fun asPayload(): Map<String, String> =
        mapOf(Keys.username to username, Keys.access to access.name)
}