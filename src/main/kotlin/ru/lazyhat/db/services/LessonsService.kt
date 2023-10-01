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
data class Lesson(val id: Int, val title: String, val start: LocalDateTime, val end: LocalDateTime)

interface LessonsService {
    suspend fun create(lesson: Lesson): Boolean
    suspend fun findById(id: Int): Lesson?
    suspend fun update(id: Int, new: (old: Lesson?) -> Lesson): Boolean

    suspend fun delete(id: Int): Boolean
}

class LessonsServiceImpl(private val database: Database) : LessonsService {
    private object Lessons : IntIdTable() {
        val title = varchar("title", 100)
        val start = datetime("start")
        val end = datetime("end")
    }

    init {
        transaction(database) {
            SchemaUtils.create(Lessons)
        }
    }

    private fun UpdateBuilder<Int>.applyLesson(lesson: Lesson) = this.apply {
        this[Lessons.title] = lesson.title
        this[Lessons.start] = lesson.start
        this[Lessons.end] = lesson.end
    }

    private fun ResultRow.toLesson(): Lesson = this.let {
        Lesson(
            it[Lessons.id].value, it[Lessons.title], it[Lessons.start], it[Lessons.end]
        )
    }


    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(lesson: Lesson): Boolean = dbQuery {
        Lessons.insert {
            it.applyLesson(lesson)
        }.insertedCount == 1
    }

    override suspend fun findById(id: Int): Lesson? = dbQuery {
        Lessons.select { Lessons.id eq id }.singleOrNull()
            ?.toLesson()

    }

    override suspend fun update(id: Int, new: (old: Lesson?) -> Lesson): Boolean = dbQuery {
        val old = findById(id)
        Lessons.update({ Lessons.id eq id }) {
            it.applyLesson(new(old))
        }
    } == 1

    override suspend fun delete(id: Int): Boolean = dbQuery {
        Lessons.deleteWhere { Lessons.id.eq(id) }
    } == 1
}
