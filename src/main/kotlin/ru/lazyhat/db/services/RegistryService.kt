package ru.lazyhat.db.services

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import ru.lazyhat.Constants
import ru.lazyhat.models.*

interface RegistryService {
    suspend fun create(registry: RegistryRecordCreate): Boolean
    suspend fun find(id: ULong): RegistryRecord?
    suspend fun findByLesson(lessonId: UInt): List<RegistryRecord>
    suspend fun findByStudent(username: String): List<RegistryRecord>
    suspend fun update(id: ULong, new: (old: RegistryRecordCreate) -> RegistryRecordCreate): Boolean
    suspend fun delete(id: ULong): Boolean
    suspend fun upsertOrDelete(update: RegistryRecordUpdate): Boolean
}

class RegistryServiceImpl(database: Database) : RegistryService {
    object Registry : IdTable<ULong>() {
        override val id: Column<EntityID<ULong>> = ulong("id").autoIncrement().entityId()
        val lessonId = uinteger("lesson_id")
        val student = varchar("student", Constants.Length.username)
        val date = date("date")
        val status = enumeration<AttendanceStatus>("status")
    }

    init {
        transaction(database) {
            SchemaUtils.create(Registry)
        }
    }

    private fun ResultRow.toRegistryRecord() = RegistryRecord(
        this[Registry.id].value,
        this[Registry.lessonId],
        this[Registry.student],
        this[Registry.date],
        this[Registry.status]
    )

    private fun UpdateBuilder<Int>.applyRegistryRecord(registry: RegistryRecordCreate) = this.apply {
        this[Registry.lessonId] = registry.lessonId
        this[Registry.student] = registry.student
        this[Registry.date] = registry.date
        this[Registry.status] = registry.attendanceStatus
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(registry: RegistryRecordCreate): Boolean = dbQuery {
        Registry.insert {
            it.applyRegistryRecord(registry)
        }.insertedCount == 1
    }

    override suspend fun find(id: ULong): RegistryRecord? = dbQuery {
        Registry.select { Registry.id eq id }.singleOrNull()?.toRegistryRecord()
    }

    override suspend fun findByLesson(lessonId: UInt): List<RegistryRecord> = dbQuery {
        Registry.select { Registry.lessonId eq lessonId }.map { it.toRegistryRecord() }
    }

    override suspend fun findByStudent(username: String): List<RegistryRecord> = dbQuery {
        Registry.select { Registry.student eq username }.map { it.toRegistryRecord() }
    }

    override suspend fun update(id: ULong, new: (old: RegistryRecordCreate) -> RegistryRecordCreate): Boolean =
        dbQuery {
            find(id)?.let { old ->
                Registry.update({ Registry.id eq id }) { it.applyRegistryRecord(new(old.toRegistryRecordCreate())) } == 1
            } ?: false
        }

    override suspend fun delete(id: ULong): Boolean = dbQuery {
        Registry.deleteWhere { Registry.id eq id }
    } == 1

    override suspend fun upsertOrDelete(update: RegistryRecordUpdate): Boolean = dbQuery {
        var count = 0
        update.recordsToUpdate.forEach { parameters ->
            val operation =
                (Registry.date eq parameters.date)
                    .and(Registry.lessonId eq update.lessonId)
                    .and(Registry.student eq parameters.student)

            if (update.newStatus != AttendanceStatus.Missing)
                if (Registry.select { operation }.empty())
                    Registry.insert {
                        it.applyRegistryRecord(
                            RegistryRecordCreate(
                                update.lessonId,
                                parameters.student,
                                parameters.date,
                                update.newStatus
                            )
                        )
                    }.let{count += it.insertedCount}
                else
                    Registry.update({ operation }) { it[status] = update.newStatus }.let { count += it }
            else
                Registry.deleteWhere { operation }.let {count += it}
        }
        count == update.recordsToUpdate.count()
    }
}