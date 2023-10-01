package ru.lazyhat.models

import kotlinx.serialization.Serializable

@Serializable
enum class Access {
    Student,
    Teacher  //TODO
}

@Serializable
data class StudentCreate(
    val username: String,
    val fullName: String,
    val password: String
)

@Serializable
data class Student(
    val username: String,
    val fullName: String,
    val password: String,
    val lessonId: Int?
)

@Serializable
data class Teacher(
    val username: String,
    val fullName: String,
    val password: String
)