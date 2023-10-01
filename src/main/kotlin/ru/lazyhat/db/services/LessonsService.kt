package ru.lazyhat.db.services

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import ru.lazyhat.Constants
import ru.lazyhat.models.Lesson

interface LessonsService {
    suspend fun create(lesson: Lesson): Boolean
    suspend fun findById(id: UInt): Lesson?
    suspend fun find(predicate: (Lesson) -> Boolean): Set<Lesson>
    suspend fun update(id: UInt, new: (old: Lesson?) -> Lesson): Boolean
    suspend fun delete(id: UInt): Boolean
}

class LessonsServiceImpl(private val database: Database) : LessonsService {
    private object Lessons : Table() {
        val id = uinteger("id")
        val username = varchar("username", Constants.Length.username)
        val title = varchar("title", Constants.Length.title)
        val groups = varchar("groups", Constants.Length.groupsList)
        val start = datetime("start")
        val end = datetime("end")
        override val primaryKey = PrimaryKey(id)
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
        this[Lessons.username] = lesson.username
        this[Lessons.groups] = Json.encodeToString(lesson.groupsList)
    }

    private fun ResultRow.toLesson(): Lesson = this.let {
        Lesson(
            it[Lessons.id],
            it[Lessons.username],
            it[Lessons.title],
            it[Lessons.start],
            it[Lessons.end],
            Json.decodeFromString(it[Lessons.groups])
        )
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(lesson: Lesson): Boolean = dbQuery {
        Lessons.insert {
            it.applyLesson(lesson)
        }.insertedCount == 1
    }

    override suspend fun findById(id: UInt): Lesson? = dbQuery {
        Lessons.select { Lessons.id eq id }.singleOrNull()
            ?.toLesson()

    }

    override suspend fun find(predicate: (Lesson) -> Boolean): Set<Lesson> = dbQuery {
        Lessons.selectAll().map { it.toLesson() }.filter(predicate).toSet()
    }

    override suspend fun update(id: UInt, new: (old: Lesson?) -> Lesson): Boolean = dbQuery {
        val old = findById(id)
        Lessons.update({ Lessons.id eq id }) {
            it.applyLesson(new(old))
        }
    } == 1

    override suspend fun delete(id: UInt): Boolean = dbQuery {
        Lessons.deleteWhere { Lessons.id.eq(id) }
    } == 1
}
