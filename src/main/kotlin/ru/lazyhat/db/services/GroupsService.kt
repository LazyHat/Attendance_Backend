package ru.lazyhat.db.services

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import ru.lazyhat.Constants
import ru.lazyhat.models.Group

interface GroupsService {
    suspend fun find(id: String): Group?
    suspend fun create(group: Group): Boolean
    suspend fun update(id: String, new: (old: Group?) -> Group): Boolean
    suspend fun delete(id: String): Boolean
}

class GroupsServiceImpl(private val database: Database) : GroupsService {
    private object Groups : Table() {
        val group = varchar("group", Constants.Length.group)
        val lessonsList = text("lessons")
    }

    init {
        transaction(database) {
            SchemaUtils.create(Groups)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun ResultRow.toGroup() = Group(
        this[Groups.group],
        Json.decodeFromString(this[Groups.lessonsList])
    )

    private fun UpdateBuilder<Int>.applyGroup(group: Group) = this.apply {
        this[Groups.group] = group.id
        this[Groups.lessonsList] = Json.encodeToString(group.lessonsList)
    }

    override suspend fun create(group: Group): Boolean = dbQuery {
        Groups.insert {
            it.applyGroup(group)
        }.insertedCount == 1
    }

    override suspend fun find(id: String): Group? = dbQuery {
        Groups.select { Groups.group eq id }.singleOrNull()?.toGroup()
    }

    override suspend fun update(id: String, new: (old: Group?) -> Group): Boolean = dbQuery {
        val old = find(id)
        Groups.update { it.applyGroup(new(old)) } == 1
    }

    override suspend fun delete(id: String): Boolean = dbQuery {
        Groups.deleteWhere { Groups.group eq id } == 1
    }
}