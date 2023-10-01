package ru.lazyhat.repository

import ru.lazyhat.db.services.LessonsService
import ru.lazyhat.models.Lesson
import ru.lazyhat.models.LessonUpdate

interface LessonsRepository {
    suspend fun createLesson(lesson: LessonUpdate): Boolean
    suspend fun getLessonsByUsername(username: String): Set<Lesson>
    suspend fun getLessonsByGroup(group: String): Set<Lesson>
    suspend fun getLessonById(id: UInt): Lesson?
}

class LessonsRepositoryImpl(
    val lessonsService: LessonsService
) : LessonsRepository {
    override suspend fun createLesson(lesson: LessonUpdate): Boolean = lessonsService.create(lesson)
    override suspend fun getLessonsByUsername(username: String): Set<Lesson> =
        lessonsService.findByUsername(username)

    override suspend fun getLessonsByGroup(group: String): Set<Lesson> =
        lessonsService.findByGroup(group)

    override suspend fun getLessonById(id: UInt): Lesson? = lessonsService.findById(id)
}