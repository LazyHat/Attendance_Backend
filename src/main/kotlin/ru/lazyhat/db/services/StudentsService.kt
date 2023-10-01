package ru.lazyhat.db.services

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import ru.lazyhat.models.Student
import ru.lazyhat.models.StudentCreate


interface StudentsService {
    suspend fun create(form: StudentCreate): Boolean
    suspend fun find(username: String): Student?
    suspend fun upsert(username: String, new: (old: Student?) -> Student): Boolean
    suspend fun delete(username: String): Boolean
}

class StudentsServiceImpl(private val database: Database) : StudentsService {
    private object Students : Table() {
        val username = varchar("username", length = 50)
        val password = varchar("password", length = 32)
        val fullName = varchar("full_name", 200)
        val lessonId = integer("lesson").nullable()

        override val primaryKey = PrimaryKey(username)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Students)
        }
    }

    private fun ResultRow.toStudent() = Student(
        this[Students.username],
        this[Students.fullName],
        this[Students.password],
        this[Students.lessonId]
    )

    private fun UpdateBuilder<Int>.applyStudent(student: Student) = this.apply {
        this[Students.username] = student.username
        this[Students.fullName] = student.fullName
        this[Students.password] = student.password
        this[Students.lessonId] = student.lessonId
    }

    private fun UpdateBuilder<Int>.applyStudent(student: StudentCreate) = this.apply {
        this[Students.username] = student.username
        this[Students.fullName] = student.fullName
        this[Students.password] = student.password
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(form: StudentCreate): Boolean =
        dbQuery {
            Students.insert {
                it.applyStudent(form)
            }.insertedCount == 1

        }

    override suspend fun find(username: String): Student? = dbQuery {
        Students.select { Students.username eq username }.singleOrNull()
            ?.toStudent()
    }

    override suspend fun upsert(username: String, new: (old: Student?) -> Student): Boolean = dbQuery {
        val old = find(username)
        Students.update({ Students.username eq username }) { it.applyStudent(new(old)) } == 1
    }

    override suspend fun delete(username: String): Boolean = dbQuery {
        Students.deleteWhere { Students.username.eq(username) }
    } == 1
}