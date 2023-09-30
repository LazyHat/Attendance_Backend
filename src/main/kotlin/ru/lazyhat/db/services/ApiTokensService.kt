package ru.lazyhat.db.services

import kotlinx.coroutines.*
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.Closeable
import java.util.*
import kotlin.time.Duration.Companion.days


@Serializable
data class ApiToken(val userLogin: String, val access: Access, val token: String, val expires: LocalDateTime) {
    enum class Access {
        Student,
        Teacher
    }
}

class ApiTokensService(private val database: Database) : Closeable {
    @OptIn(DelicateCoroutinesApi::class)
    private val context = newSingleThreadContext("tokens")
    private val scope = CoroutineScope(context)

    private object ApiTokens : Table() {
        val access = enumeration("access", ApiToken.Access::class)
        val userLogin = varchar("user_login", 50)
        val token = uuid("token").clientDefault { UUID.randomUUID() }
        val expires = datetime("expires").clientDefault {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime().plusDays(3)
                .toKotlinLocalDateTime()
        }
        override val primaryKey = PrimaryKey(userLogin, access)

    }

    init {
        transaction(database) {
            SchemaUtils.create(ApiTokens)
        }
        scope.launch {
            while (true) {
                checkTokensIfExpires()
                delay(1.days)
            }
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }


    suspend fun create(login: String, access: ApiToken.Access): ApiToken = dbQuery {
        if (ApiTokens.select { ApiTokens.userLogin eq login }.singleOrNull() != null) {
            delete(login, access)
        }
        ApiTokens.insert {
            it[userLogin] = login
            it[ApiTokens.access] = access
        }.let {
            ApiToken(it[ApiTokens.userLogin], it[ApiTokens.access], it[ApiTokens.token].toString(), it[ApiTokens.expires])
        }
    }

    suspend fun read(token: String): ApiToken? = dbQuery {
        ApiTokens.select { ApiTokens.token eq UUID.fromString(token) }.singleOrNull()?.let {
            ApiToken(
                it[ApiTokens.userLogin],
                it[ApiTokens.access],
                it[ApiTokens.token].toString(),
                it[ApiTokens.expires]
            )
        }
    }

    suspend fun delete(login: String, access: ApiToken.Access) {
        dbQuery {
            ApiTokens.deleteWhere { (userLogin eq login) and (ApiTokens.access eq access) }
        }
    }

    override fun close() {
        context.close()
    }

    private suspend fun checkTokensIfExpires() {
        dbQuery {
            ApiTokens.deleteWhere {
                expires less Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }
        }
    }
}
