package ru.lazyhat.db.services

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class StudentCreateForm(val username: String, val pass: String, val fullName: String)

@Serializable
data class Student(val username: String, val fullName: String, val pass: String, val lessonId: Int?)

class StudentsService(private val database: Database) {
    object Students : Table() {
        val username = varchar("username", length = 50)
        val pass = varchar("pass", length = 50)
        val fullName = varchar("full_name", 200)
        val lessonId = integer("lesson").nullable()
        override val primaryKey = PrimaryKey(username)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Students)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(form: StudentCreateForm): String = dbQuery {
        Students.insert {
            it[username] = form.username
            it[pass] = form.pass
            it[fullName] = form.fullName
        }[Students.username]
    }

    suspend fun isEmpty(): Boolean = dbQuery { //TODO delete method
        Students.selectAll().empty()
    }

    suspend fun authentificate(username: String, pass: String): Boolean = dbQuery {
        Students.select { Students.username eq username}.singleOrNull()?.let {
            it[Students.pass] == pass
        } ?: false
    }

    suspend fun read(username: String): Student? {
        return dbQuery {
            Students.select { Students.username eq username }.singleOrNull()
                ?.let { Student(it[Students.username], it[Students.fullName], it[Students.pass], it[Students.lessonId]) }
        }
    }

    suspend fun update(username: String, user: Student) {
        dbQuery {
            Students.update({ Students.username eq username }) {
                it[Students.username] = username
                it[fullName] = user.fullName
            }
        }
    }

    suspend fun delete(username: String) {
        dbQuery {
            Students.deleteWhere { Students.username.eq(username) }
        }
    }
}