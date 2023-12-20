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
    suspend fun updateLesson(lessonId: UInt, lessonUpdate: LessonUpdate): Boolean
    suspend fun getLessonAttendance(id: UInt): LessonAttendance
    suspend fun getAllTeachers(): List<Teacher>
    suspend fun createTeacher(teacher: Teacher): Boolean
    suspend fun getAllStudents(): List<Student>
    suspend fun createStudent(form: StudentCreate): Boolean
    suspend fun deleteStudent(username: String): Boolean
    suspend fun deleteTeacher(username: String): Boolean
    suspend fun putSomeStudents(students: List<StudentCreate>): Boolean
}

class AdminRepositoryImpl(
    private val registryRepository: RegistryRepository,
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
    override suspend fun updateLesson(lessonId: UInt, lessonUpdate: LessonUpdate): Boolean =
        lessonsService.update(lessonId) { lessonUpdate }

    override suspend fun getLessonAttendance(id: UInt): LessonAttendance = registryRepository.getAttendanceByLesson(id)
    override suspend fun getAllTeachers(): List<Teacher> = teachersService.getAll()
    override suspend fun createTeacher(teacher: Teacher): Boolean = teachersService.create(teacher)
    override suspend fun getAllStudents(): List<Student> = studentsService.getAll()
    override suspend fun createStudent(form: StudentCreate): Boolean = studentsService.insert(form)
    override suspend fun deleteStudent(username: String): Boolean = studentsService.delete(username)
    override suspend fun deleteTeacher(username: String): Boolean = teachersService.delete(username)
    override suspend fun putSomeStudents(students: List<StudentCreate>): Boolean = studentsService.insertList(students)
}