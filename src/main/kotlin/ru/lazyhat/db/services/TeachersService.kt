package ru.lazyhat.db.services

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import ru.lazyhat.Constants
import ru.lazyhat.models.Teacher


interface TeachersService {
    suspend fun create(form: Teacher): Boolean
    suspend fun getAll(): List<Teacher>
    suspend fun find(username: String): Teacher?
    suspend fun update(username: String, new: (old: Teacher) -> Teacher): Boolean
    suspend fun delete(username: String): Boolean
}

class TeachersServiceImpl(database: Database) : TeachersService {
    private object Teachers : Table() {
        val username = varchar("username", Constants.Length.username)
        val fullName = varchar("full_name", Constants.Length.fullname)
        val password = varchar("password", Constants.Length.password)
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

    override suspend fun getAll(): List<Teacher> = dbQuery {
        Teachers.selectAll().map { it.toTeacher() }
    }

    override suspend fun find(username: String): Teacher? = dbQuery {
        Teachers.select { Teachers.username eq username }.singleOrNull()?.toTeacher()
    }

    override suspend fun update(username: String, new: (old: Teacher) -> Teacher): Boolean = dbQuery {
        find(username)?.let { old ->
            Teachers.update({ Teachers.username eq username }) { it.applyTeacher(new(old)) } == 1
        } ?: false
    }

    override suspend fun delete(username: String): Boolean = dbQuery {
        Teachers.deleteWhere { Teachers.username.eq(username) }
    } == 1
}