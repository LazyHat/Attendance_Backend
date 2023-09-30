package ru.lazyhat.db.services

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class Lesson(val title: String, val start: LocalDateTime, val end: LocalDateTime)

class LessonsService(private val database: Database) {
    object Lessons : IntIdTable() {
        val title = varchar("title", 100)
        val start = datetime("start")
        val end = datetime("end")
    }

    init {
        transaction(database) {
            SchemaUtils.create(Lessons)
        }
    }

    private fun UpdateBuilder<Int>.update(lesson: Lesson) {
        set(Lessons.title, lesson.title)
        set(Lessons.start, lesson.start)
        set(Lessons.end, lesson.end)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(lesson: Lesson): Int = dbQuery {
        Lessons.insert {
            it.update(lesson)
        }[Lessons.id].value
    }

    suspend fun read(id: Int): Lesson? {
        return dbQuery {
            Lessons.select { Lessons.id eq id }
                .map { Lesson(it[Lessons.title], it[Lessons.start], it[Lessons.end]) }
                .singleOrNull()
        }
    }

    suspend fun update(id: Int, lesson: Lesson) {
        dbQuery {
            Lessons.update({ Lessons.id eq id }) {
                it.update(lesson)
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            Lessons.deleteWhere { Lessons.id.eq(id) }
        }
    }
}
