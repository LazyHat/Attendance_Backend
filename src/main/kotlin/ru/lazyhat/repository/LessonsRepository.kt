package ru.lazyhat.repository

import ru.lazyhat.db.services.LessonTokensService
import ru.lazyhat.db.services.LessonsService
import ru.lazyhat.models.Lesson
import ru.lazyhat.models.LessonToken
import ru.lazyhat.models.LessonUpdate

interface LessonsRepository {
    suspend fun createLesson(lesson: LessonUpdate): Boolean
    suspend fun getLessonsByUsername(username: String): Set<Lesson>
    suspend fun getLessonsByGroup(group: String): Set<Lesson>
    suspend fun getLessonById(id: UInt): Lesson?
    suspend fun createToken(id: UInt): LessonToken
    suspend fun getTokenInfo(id: String): LessonToken?
    suspend fun getLessonByToken(id: String): Lesson?
}

class LessonsRepositoryImpl(
    val lessonsService: LessonsService,
    val lessonTokensService: LessonTokensService
) : LessonsRepository {
    override suspend fun createLesson(lesson: LessonUpdate): Boolean = lessonsService.create(lesson)
    override suspend fun getLessonsByUsername(username: String): Set<Lesson> =
        lessonsService.findByUsername(username)

    override suspend fun getLessonsByGroup(group: String): Set<Lesson> =
        lessonsService.findByGroup(group)

    override suspend fun getLessonById(id: UInt): Lesson? = lessonsService.findById(id)
    override suspend fun createToken(id: UInt): LessonToken = lessonTokensService.create(id)

    override suspend fun getTokenInfo(id: String): LessonToken? = lessonTokensService.find(id)

    override suspend fun getLessonByToken(id: String): Lesson? = lessonTokensService.find(id)?.let {
        lessonsService.findById(it.lessonId)
    }
}