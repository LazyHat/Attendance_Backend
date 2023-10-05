package ru.lazyhat.db.services

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
    suspend fun find(id: UUID): LessonToken?
}

class LessonTokensServiceImpl(database: Database) : LessonTokensService {
    private object LessonTokens : Table() {
        val id = uuid("id").clientDefault { UUID.randomUUID() }
        val lessonId = uinteger("lesson_id")
        val expires =
            datetime("expires").clientDefault {
                Clock.System.now().plus(Constants.TokensLives.lesson).toLocalDateTime(TimeZone.currentSystemDefault())
            }
        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(LessonTokens)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun UpdateBuilder<Int>.update(lessonId: UInt) {
        set(LessonTokens.lessonId, lessonId)
    }

    private suspend fun delete(lessonId: UInt) {
        dbQuery {
            LessonTokens.deleteWhere { LessonTokens.lessonId.eq(lessonId) }
        }
    }

    private suspend fun checkTokensIfExpires() {
        dbQuery {
            LessonTokens.deleteWhere {
                expires less Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }
        }
    }

    override suspend fun create(lessonId: UInt): LessonToken = dbQuery {
        checkTokensIfExpires()
        if (LessonTokens.select { LessonTokens.lessonId eq lessonId }.singleOrNull() != null) {
            delete(lessonId)
        }
        LessonTokens.insert {
            it.update(lessonId)
        }.let {
            LessonToken(it[LessonTokens.id].toString(), it[LessonTokens.lessonId], it[LessonTokens.expires])
        }
    }

    override suspend fun find(id: UUID): LessonToken? = dbQuery {
        checkTokensIfExpires()
        LessonTokens.select { LessonTokens.id eq id }.singleOrNull()?.let {
            LessonToken(it[LessonTokens.id].toString(), it[LessonTokens.lessonId], it[LessonTokens.expires])
        }
    }
}
