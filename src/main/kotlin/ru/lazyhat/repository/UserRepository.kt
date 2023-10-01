package ru.lazyhat.repository

import ru.lazyhat.db.services.StudentsService
import ru.lazyhat.db.services.TeachersService
import ru.lazyhat.models.Access
import ru.lazyhat.models.Student
import ru.lazyhat.models.StudentCreate
import ru.lazyhat.models.Teacher
import ru.lazyhat.plugins.JWTAuth

interface UserRepository {
    suspend fun logIn(username: String, password: String, access: Access): String?
    suspend fun registerTeacher(teacher: Teacher): Boolean
    suspend fun registerStudent(studentCreate: StudentCreate): Boolean
    suspend fun findStudentByUsername(username: String): Student?
    suspend fun findTeacherByUsername(username: String): Teacher?
}

class UserRepositoryImpl(
    val studentsService: StudentsService,
    val teachersService: TeachersService,
    val jwtAuth: JWTAuth
) : UserRepository {
    override suspend fun logIn(username: String, password: String, access: Access): String? =
        if (when (access) {
                Access.Student -> {
                    studentsService.find(username)?.password == password
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
    override suspend fun findStudentByUsername(username: String): Student? = studentsService.find(username)
    override suspend fun findTeacherByUsername(username: String): Teacher? = teachersService.find(username)
}
