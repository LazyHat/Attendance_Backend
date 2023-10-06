package ru.lazyhat.db.services

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import ru.lazyhat.Constants
import ru.lazyhat.models.Student
import ru.lazyhat.models.StudentCreate


interface StudentsService {
    suspend fun insert(form: StudentCreate): Boolean
    suspend fun getAll(): List<Student>
    suspend fun findByUsername(username: String): Student?
    suspend fun update(username: String, new: (old: Student) -> Student): Boolean
    suspend fun delete(username: String): Boolean
    suspend fun findByGroup(group: String): List<Student>
    suspend fun insertList(list: List<StudentCreate>): Boolean
}

class StudentsServiceImpl(database: Database) : StudentsService {
    object Students : IdTable<String>() {
        override val id: Column<EntityID<String>> = varchar("username", Constants.Length.username).entityId()
        val fullName = varchar("full_name", Constants.Length.fullname)
        val password = varchar("password", Constants.Length.password)
        val groupId = varchar("group", Constants.Length.group)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Students)
        }
    }

    private fun ResultRow.toStudent() = Student(
        this[Students.id].value,
        this[Students.fullName],
        this[Students.password],
        this[Students.groupId]
    )

    private fun UpdateBuilder<Int>.applyStudent(student: Student) = this.apply {
        this[Students.id] = student.username
        this[Students.fullName] = student.fullName
        this[Students.password] = student.password
        this[Students.groupId] = student.groupId
    }

    private fun UpdateBuilder<Int>.applyStudent(student: StudentCreate) = this.apply {
        this[Students.id] = student.username
        this[Students.fullName] = student.fullName
        this[Students.password] = student.password
        this[Students.groupId] = student.groupId
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun insert(form: StudentCreate): Boolean =
        dbQuery {
            Students.insert {
                it.applyStudent(form)
            }.insertedCount == 1

        }

    override suspend fun getAll(): List<Student> = dbQuery {
        Students.selectAll().map { it.toStudent() }
    }

    override suspend fun findByUsername(username: String): Student? = dbQuery {
        Students.select { Students.id eq username }.singleOrNull()
            ?.toStudent()
    }

    override suspend fun findByGroup(group: String): List<Student> = dbQuery {
        Students.select { Students.groupId eq group }.map { it.toStudent() }
    }

    override suspend fun insertList(list: List<StudentCreate>): Boolean = dbQuery {
        Students.batchInsert(list){
            applyStudent(it)
        }.count() == list.count()
    }

    override suspend fun update(username: String, new: (old: Student) -> Student): Boolean = dbQuery {
        findByUsername(username)?.let { old ->
            Students.update({ Students.id eq username }) { it.applyStudent(new(old)) } == 1
        } ?: false
    }

    override suspend fun delete(username: String): Boolean = dbQuery {
        Students.deleteWhere { id eq username }
    } == 1
}