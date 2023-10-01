package ru.lazyhat.repository

import ru.lazyhat.db.services.LessonsService
import ru.lazyhat.models.Lesson

interface LessonsRepository {
    suspend fun createLesson(lesson: Lesson): Boolean
    suspend fun getLessonsByUsername(username: String): Set<Lesson>
    suspend fun getLessonsByGroup(group: String): Set<Lesson>
}

class LessonsRepositoryImpl(
    val lessonsService: LessonsService
) : LessonsRepository {
    override suspend fun createLesson(lesson: Lesson): Boolean = lessonsService.create(lesson)
    override suspend fun getLessonsByUsername(username: String): Set<Lesson> =
        lessonsService.find { it.username == username }
    override suspend fun getLessonsByGroup(group: String): Set<Lesson> =
        lessonsService.find { it.groupsList.contains(group) }
}