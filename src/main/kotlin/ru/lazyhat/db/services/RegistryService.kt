package ru.lazyhat.db.services

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import ru.lazyhat.Constants
import ru.lazyhat.models.RegistryRecord
import ru.lazyhat.models.RegistryRecordCreate
import ru.lazyhat.models.toRegistryRecordCreate

interface RegistryService {
    suspend fun create(registry: RegistryRecordCreate): Boolean
    suspend fun find(id: ULong): RegistryRecord?
    suspend fun findByLesson(lessonId: UInt): List<RegistryRecord>
    suspend fun findByStudent(username: String): List<RegistryRecord>
    suspend fun update(id: ULong, new: (old: RegistryRecordCreate) -> RegistryRecordCreate): Boolean
    suspend fun delete(id: ULong): Boolean
}

class RegistryServiceImpl(database: Database) : RegistryService {
    private object Registry : Table() {
        val id = ulong("id")
        val lessonId = uinteger("lesson_id")
        val student = varchar("student", Constants.Length.username)
        val createdAt = datetime("created_at")
        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Registry)
        }
    }

    private fun ResultRow.toRegistryRecord() = RegistryRecord(
        this[Registry.id],
        this[Registry.lessonId],
        this[Registry.student],
        this[Registry.createdAt],
    )

    private fun UpdateBuilder<Int>.applyRegistryRecord(registry: RegistryRecordCreate) = this.apply {
        this[Registry.lessonId] = registry.lessonId
        this[Registry.student] = registry.student
        this[Registry.createdAt] = registry.createdAt
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(registry: RegistryRecordCreate): Boolean =
        dbQuery {
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

    override suspend fun update(id: ULong, new: (old: RegistryRecordCreate) -> RegistryRecordCreate): Boolean = dbQuery {
        find(id)?.let { old ->
            Registry.update({ Registry.id eq id }) { it.applyRegistryRecord(new(old.toRegistryRecordCreate())) } == 1
        } ?: false
    }

    override suspend fun delete(id: ULong): Boolean = dbQuery {
        Registry.deleteWhere { Registry.id eq id }
    } == 1
}