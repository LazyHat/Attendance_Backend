package ru.lazyhat.repository

import ru.lazyhat.db.services.LessonsService
import ru.lazyhat.db.services.StudentsService
import ru.lazyhat.db.services.TeachersService
import ru.lazyhat.models.*
import ru.lazyhat.plugins.JWTAuth

interface AdminRepository {
    fun validateSuperUser(credentials: Credentials): Boolean
    fun createUserToken(credentials: Credentials): String
    suspend fun getAllLessons(): List<Lesson>
    suspend fun getLessonById(id: UInt): Lesson?
    suspend fun createLesson(lessonUpdate: LessonUpdate): Boolean
    suspend fun getAllTeachers(): List<Teacher>
    suspend fun getAllStudents(): List<Student>
}

class AdminRepositoryImpl(
    private val teachersService: TeachersService,
    private val studentsService: StudentsService,
    private val lessonsService: LessonsService,
    private val jwtAuth: JWTAuth,
    private val superUser: Credentials
) : AdminRepository {
    override fun validateSuperUser(credentials: Credentials): Boolean = credentials == superUser
    override fun createUserToken(credentials: Credentials): String =
        jwtAuth.generateToken(credentials.username, Access.Admin, credentials.password)
    override suspend fun getAllLessons(): List<Lesson> = lessonsService.getAll()
    override suspend fun getLessonById(id: UInt): Lesson? = lessonsService.findById(id)
    override suspend fun createLesson(lessonUpdate: LessonUpdate): Boolean = lessonsService.create(lessonUpdate)
    override suspend fun getAllTeachers(): List<Teacher> {
        TODO("Not yet implemented")
    }
    override suspend fun getAllStudents(): List<Student> {
        TODO("Not yet implemented")
    }
}