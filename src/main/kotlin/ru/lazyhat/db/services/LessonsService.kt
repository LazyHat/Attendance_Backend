package ru.lazyhat.db.services

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.duration
import org.jetbrains.exposed.sql.kotlin.datetime.time
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import ru.lazyhat.Constants
import ru.lazyhat.models.Lesson
import ru.lazyhat.models.LessonUpdate

interface LessonsService {
    suspend fun create(lesson: LessonUpdate): Boolean
    suspend fun getAll(): List<Lesson>
    suspend fun findById(id: UInt): Lesson?
    suspend fun findByUsername(username: String): List<Lesson>
    suspend fun findByGroup(group: String): List<Lesson>
    suspend fun update(id: UInt, new: (old: LessonUpdate?) -> LessonUpdate): Boolean
    suspend fun delete(id: UInt): Boolean
}

class LessonsServiceImpl(database: Database) : LessonsService {
    private object Lessons : Table() {
        val id = uinteger("id").autoIncrement()
        val username = varchar("username", Constants.Length.username)
        val title = varchar("title", Constants.Length.title)
        val groups = varchar("groups", Constants.Length.groupsList)
        val dayOfWeek = enumeration<DayOfWeek>("day_of_week")
        val start = time("start_time")
        val duration = duration("duration")
        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Lessons)
        }
    }


    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(lesson: LessonUpdate): Boolean = dbQuery {
        Lessons.insert {
            it.applyLesson(lesson)
        }.insertedCount == 1
    }

    override suspend fun getAll(): List<Lesson> = dbQuery {
        Lessons.selectAll().map { it.toLesson() }
    }

    override suspend fun findById(id: UInt): Lesson? = dbQuery {
        Lessons.select { Lessons.id eq id }.singleOrNull()
            ?.toLesson()

    }

    override suspend fun findByUsername(username: String): List<Lesson> = dbQuery {
        Lessons.select { Lessons.username eq username }.map { it.toLesson() }
    }

    override suspend fun findByGroup(group: String): List<Lesson> = dbQuery {
        Lessons.selectAll().map { it.toLesson() }.filter { it.groupsList.contains(group) }
    }

    override suspend fun update(id: UInt, new: (old: LessonUpdate?) -> LessonUpdate): Boolean = dbQuery {
        val old = findById(id)?.toLessonUpdate()
        Lessons.update({ Lessons.id eq id }) {
            it.applyLesson(new(old))
        }
    } == 1

    override suspend fun delete(id: UInt): Boolean = dbQuery {
        Lessons.deleteWhere { Lessons.id.eq(id) }
    } == 1

    private fun UpdateBuilder<Int>.applyLesson(lesson: LessonUpdate) = this.apply {
        this[Lessons.title] = lesson.title
        this[Lessons.start] = lesson.start
        this[Lessons.duration] = lesson.duration
        this[Lessons.dayOfWeek] = lesson.dayOfWeek
        this[Lessons.username] = lesson.username
        this[Lessons.groups] = Json.encodeToString(lesson.groupsList)
    }

    private fun ResultRow.toLesson(): Lesson = this.let {
        Lesson(
            it[Lessons.id],
            it[Lessons.username],
            it[Lessons.title],
            it[Lessons.dayOfWeek],
            it[Lessons.start],
            it[Lessons.duration],
            Json.decodeFromString(it[Lessons.groups])
        )
    }

    private fun Lesson.toLessonUpdate(): LessonUpdate = LessonUpdate(
        username,
        title,
        dayOfWeek,
        start,
        duration,
        groupsList
    )
}
