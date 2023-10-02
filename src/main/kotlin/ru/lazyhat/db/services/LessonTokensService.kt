package ru.lazyhat.db.services

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import ru.lazyhat.Constants
import ru.lazyhat.models.LessonToken
import java.util.*


interface LessonTokensService {
    suspend fun create(lessonId: UInt): LessonToken
    suspend fun find(id: String): LessonToken?
}

class LessonTokensServiceImpl(database: Database) : LessonTokensService {
    private object QrCodeTokens : Table() {
        val id = uuid("id").clientDefault { UUID.randomUUID() }
        val lessonId = uinteger("lesson_id")
        val expires =
            datetime("expires").clientDefault {
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime()
                    .plusSeconds(Constants.TokensLives.lesson.inWholeSeconds)
                    .toKotlinLocalDateTime()
            }
        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(QrCodeTokens)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun UpdateBuilder<Int>.update(lessonId: UInt) {
        set(QrCodeTokens.lessonId, lessonId)
    }

    private suspend fun delete(lessonId: UInt) {
        dbQuery {
            QrCodeTokens.deleteWhere { QrCodeTokens.lessonId.eq(lessonId) }
        }
    }

    private suspend fun checkTokensIfExpires() {
        dbQuery {
            QrCodeTokens.deleteWhere {
                expires less Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }
        }
    }

    override suspend fun create(lessonId: UInt): LessonToken = dbQuery {
        checkTokensIfExpires()
        if (QrCodeTokens.select { QrCodeTokens.lessonId eq lessonId }.singleOrNull() != null) {
            delete(lessonId)
        }
        QrCodeTokens.insert {
            it.update(lessonId)
        }.let {
            LessonToken(it[QrCodeTokens.id].toString(), it[QrCodeTokens.lessonId], it[QrCodeTokens.expires])
        }
    }

    override suspend fun find(id: String): LessonToken? = dbQuery {
        checkTokensIfExpires()
        QrCodeTokens.select { QrCodeTokens.id eq UUID.fromString(id) }.singleOrNull()?.let {
            LessonToken(it[QrCodeTokens.id].toString(), it[QrCodeTokens.lessonId], it[QrCodeTokens.expires])
        }
    }
}
