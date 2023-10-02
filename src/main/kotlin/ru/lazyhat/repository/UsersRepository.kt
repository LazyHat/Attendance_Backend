package ru.lazyhat.repository

import ru.lazyhat.db.services.StudentsService
import ru.lazyhat.db.services.TeachersService
import ru.lazyhat.models.*
import ru.lazyhat.plugins.JWTAuth

interface UsersRepository {
    suspend fun logIn(username: String, password: String, access: Access): String?
    suspend fun registerTeacher(teacher: Teacher): Boolean
    suspend fun registerStudent(studentCreate: StudentCreate): Boolean
    suspend fun findStudentByUsername(username: String): Student?
    suspend fun findTeacherByUsername(username: String): Teacher?
    suspend fun findStudentsByGroup(group: String): Set<Student>
    suspend fun updateStudentStatus(username: String, new: Status): Boolean
}

class UsersRepositoryImpl(
    val studentsService: StudentsService,
    val teachersService: TeachersService,
    val jwtAuth: JWTAuth
) : UsersRepository {
    override suspend fun logIn(username: String, password: String, access: Access): String? =
        if (when (access) {
                Access.Student -> {
                    studentsService.findByUsername(username)?.password == password
                }

                Access.Teacher -> {
                    teachersService.find(username)?.password == password
                }
            }
        ) {
            jwtAuth.generateToken(username, access)
        } else null

    override suspend fun registerTeacher(teacher: Teacher): Boolean = teachersService.create(teacher)
    override suspend fun registerStudent(studentCreate: StudentCreate): Boolean = studentsService.create(studentCreate)
    override suspend fun findStudentByUsername(username: String): Student? = studentsService.findByUsername(username)
    override suspend fun findTeacherByUsername(username: String): Teacher? = teachersService.find(username)
    override suspend fun findStudentsByGroup(group: String): Set<Student> = studentsService.findByGroup(group)
    override suspend fun updateStudentStatus(username: String, new: Status): Boolean =
        studentsService.update(username) {
            it.copy(status = new)
        }
}
