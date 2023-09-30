package ru.lazyhat.db.services
//
//import kotlinx.coroutines.Dispatchers
//import kotlinx.serialization.Serializable
//import org.jetbrains.exposed.sql.statements.UpdateBuilder
//import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
//import org.jetbrains.exposed.sql.transactions.transaction
//
//@Serializable
//data class UserCreateForm(val login: String, val pass: String, val fullName: String)
//
//@Serializable
//data class User(val login: String, val fullName: String, val lessonId: Int?)
//
//class UserService(private val database: Database) {
//    object Users : Table() {
//        val login = varchar("login", length = 50)
//        val pass = varchar("pass", length = 50)
//        val fullName = varchar("full_name", 200)
//        val lessonId = integer("lesson").nullable()
//        override val primaryKey = PrimaryKey(login)
//    }
//
//    init {
//        transaction(database) {
//            SchemaUtils.create(Users)
//        }
//    }
//
//    suspend fun <T> dbQuery(block: suspend () -> T): T =
//        newSuspendedTransaction(Dispatchers.IO) { block() }
//
//    private fun UpdateBuilder<Int>.update(user: UserNew) {
//        set(Users.login, user.login)
//        set(Users.pass, user.pass)
//        set(Users.fullName, user.fullName)
//    }
//
//    suspend fun create(user: UserNew): String = dbQuery {
//        Users.insert {
//            it.update(user)
//        }[Users.login]
//    }
//
//    suspend fun isEmpty(): Boolean = dbQuery { //TODO delete method
//        Users.selectAll().empty()
//    }
//
//    suspend fun read(login: String): UserResult? {
//        return dbQuery {
//            Users.select { Users.login eq login }
//                .map { UserResult(it[Users.fullName], it[Users.lessonId]) }
//                .singleOrNull()
//        }
//    }
//
//    suspend fun getRegisteredUsers(lessonId: Int): List<UserResult> = dbQuery {
//        Users.select { Users.lessonId eq lessonId}.map {
//            UserResult(
//                it[Users.fullName],
//                it[Users.lessonId]
//            )
//        }
//    }
//
//    suspend fun update(login: String, user: UserResult) {
//        dbQuery {
//            Users.update({ Users.login eq login }) {
//                it[Users.login] = login
//                it[fullName] = user.fullName
//            }
//        }
//    }
//
//    suspend fun setLesson(login: String, lessonId: Int?) = dbQuery {
//        Users.update({ Users.login eq login }) {
//            it[Users.lessonId] = lessonId
//        }
//    }
//
//    suspend fun delete(login: String) {
//        dbQuery {
//            Users.deleteWhere { Users.login.eq(login) }
//        }
//    }
//}
