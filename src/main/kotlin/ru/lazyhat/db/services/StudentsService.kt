package ru.lazyhat.db.services

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class StudentCreateForm(val login: String, val pass: String, val fullName: String)

@Serializable
data class Student(val login: String, val fullName: String, val lessonId: Int?)

class StudentsService(private val database: Database) {
    object Students : Table() {
        val login = varchar("login", length = 50)
        val pass = varchar("pass", length = 50)
        val fullName = varchar("full_name", 200)
        val lessonId = integer("lesson").nullable()
        override val primaryKey = PrimaryKey(login)
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
            it[login] = form.login
            it[pass] = form.pass
            it[fullName] = form.fullName
        }[Students.login]
    }

    suspend fun isEmpty(): Boolean = dbQuery { //TODO delete method
        Students.selectAll().empty()
    }

    suspend fun authentificate(login: String, pass: String): Boolean = dbQuery {
        Students.select { Students.login eq login }.singleOrNull()?.let{
            it[Students.pass] == pass
        } ?: false
    }

    suspend fun read(login: String): Student? {
        return dbQuery {
            Students.select { Students.login eq login }.singleOrNull()
                ?.let { Student(it[Students.login], it[Students.fullName], it[Students.lessonId]) }
        }
    }

    suspend fun update(login: String, user: Student) {
        dbQuery {
            Students.update({ Students.login eq login }) {
                it[Students.login] = login
                it[fullName] = user.fullName
            }
        }
    }

    suspend fun delete(login: String) {
        dbQuery {
            Students.deleteWhere { Students.login.eq(login) }
        }
    }
}