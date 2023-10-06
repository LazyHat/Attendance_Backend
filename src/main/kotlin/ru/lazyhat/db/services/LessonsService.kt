package ru.lazyhat.db.services

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.date
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
    object Lessons : IdTable<UInt>() {
        override val id: Column<EntityID<UInt>> = uinteger("id").autoIncrement().entityId()
        val teacher = varchar("teacher", Constants.Length.username)
        val title = varchar("title", Constants.Length.title)
        val dayOfWeek = enumeration<DayOfWeek>("day_of_week")
        val startTime = time("start_time")
        val durationHours = uinteger("duration_hours")
        val startDate = date("start_date")
        val durationWeeks = uinteger("duration_weeks")
        val groups = varchar("groups", Constants.Length.groupsList)
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
        Lessons.select { Lessons.teacher eq username }.map { it.toLesson() }
    }

    override suspend fun findByGroup(group: String): List<Lesson> = dbQuery {
        Lessons.selectAll().map { it.toLesson() }.filter { it.groups.contains(group) }
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
        this[Lessons.teacher] = lesson.teacher
        this[Lessons.title] = lesson.title
        this[Lessons.dayOfWeek] = lesson.dayOfWeek
        this[Lessons.startTime] = lesson.startTime
        this[Lessons.durationHours] = lesson.durationHours
        this[Lessons.startDate] = lesson.startDate
        this[Lessons.durationWeeks] = lesson.durationWeeks
        this[Lessons.groups] = Json.encodeToString(lesson.groups)
    }

    private fun ResultRow.toLesson(): Lesson = this.let {
        Lesson(
            it[Lessons.id].value,
            it[Lessons.teacher],
            it[Lessons.title],
            it[Lessons.dayOfWeek],
            it[Lessons.startTime],
            it[Lessons.durationHours],
            it[Lessons.startDate],
            it[Lessons.durationWeeks],
            Json.decodeFromString(it[Lessons.groups])
        )
    }

    private fun Lesson.toLessonUpdate(): LessonUpdate = LessonUpdate(
        teacher,
        title,
        dayOfWeek,
        startTime,
        durationHours,
        startDate,
        durationWeeks,
        groups
    )
}
