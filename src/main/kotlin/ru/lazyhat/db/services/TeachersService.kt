package ru.lazyhat.db.services

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import ru.lazyhat.models.Teacher


interface TeachersService {
    suspend fun create(form: Teacher): Boolean
    suspend fun find(username: String): Teacher?
    suspend fun upsert(username: String, new: (old: Teacher?) -> Teacher): Boolean
    suspend fun delete(username: String): Boolean
}

class TeachersServiceImpl(private val database: Database) : TeachersService {
    private object Teachers : Table() {
        val username = varchar("username", length = 50)
        val password = varchar("password", length = 32)
        val fullName = varchar("full_name", 200)
        override val primaryKey = PrimaryKey(username)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Teachers)
        }
    }

    private fun ResultRow.toTeacher() = Teacher(
        this[Teachers.username],
        this[Teachers.fullName],
        this[Teachers.password]
    )

    private fun UpdateBuilder<Int>.applyTeacher(teacher: Teacher) = this.apply {
        this[Teachers.username] = teacher.username
        this[Teachers.fullName] = teacher.fullName
        this[Teachers.password] = teacher.password
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(form: Teacher): Boolean =
        dbQuery {
            Teachers.insert {
                it.applyTeacher(form)
            }.insertedCount == 1
        }

    override suspend fun find(username: String): Teacher? = dbQuery {
        Teachers.select { Teachers.username eq username }.singleOrNull()?.toTeacher()
    }

    override suspend fun upsert(username: String, new: (old: Teacher?) -> Teacher): Boolean = dbQuery {
        val old = find(username)
        Teachers.update({ Teachers.username eq username }) { it.applyTeacher(new(old)) } == 1
    }

    override suspend fun delete(username: String): Boolean = dbQuery {
        Teachers.deleteWhere { Teachers.username.eq(username) }
    } == 1
}