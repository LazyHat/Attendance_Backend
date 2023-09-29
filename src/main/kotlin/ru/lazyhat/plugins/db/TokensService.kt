package ru.lazyhat.plugins.db

import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.time.Duration.Companion.seconds

@Serializable
data class Token(val id: String, val lessonId: Int, val expires: LocalDateTime)

class TokensService(private val database: Database) : Closeable {
    @OptIn(DelicateCoroutinesApi::class)
    private val context = newSingleThreadContext("tokens")
    private val scope = CoroutineScope(context)

    object Tokens : Table() {
        val id = uuid("id").clientDefault { UUID.randomUUID() }
        val lessonId = integer("lesson_id")
        val expires =
            datetime("expires").clientDefault {
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime().plusMinutes(3)
                    .toKotlinLocalDateTime()
            }
        override val primaryKey = PrimaryKey(id)

    }

    init {
        transaction(database) {
            SchemaUtils.create(Tokens)
        }
        scope.launch {
            while (true) {
                checkTokensIfExpires()
                delay(10.seconds)
            }
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun UpdateBuilder<Int>.update(lessonId: Int) {
        set(Tokens.lessonId, lessonId)
    }

    suspend fun create(lessonId: Int): Token = dbQuery {
        if (Tokens.select { Tokens.lessonId eq lessonId }.singleOrNull() != null) {
            delete(lessonId)
        }
        Tokens.insert {
            it.update(lessonId)
        }.let {
            Token(it[Tokens.id].toString(), it[Tokens.lessonId], it[Tokens.expires])
        }
    }

    suspend fun read(id: String): Token? = dbQuery {
        Tokens.select { Tokens.id eq UUID.fromString(id) }.singleOrNull()?.let {
            Token(it[Tokens.id].toString(), it[Tokens.lessonId], it[Tokens.expires])
        }
    }

    suspend fun delete(lessonId: Int) {
        dbQuery {
            Tokens.deleteWhere { Tokens.lessonId.eq(lessonId) }
        }
    }

    override fun close() {
        context.close()
    }

    private suspend fun checkTokensIfExpires() {
        dbQuery {
            Tokens.deleteWhere {
                expires less Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }
        }
    }
}
