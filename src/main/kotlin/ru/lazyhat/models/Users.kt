package ru.lazyhat.models

import kotlinx.serialization.Serializable

@Serializable
enum class Access {
    Student,
    Teacher
}

@Serializable
enum class Status {
    Idle,
    InLesson
}

@Serializable
data class StudentCreate(
    val username: String,
    val fullName: String,
    val password: String,
    val groupId: String
)

@Serializable
data class Student(
    val username: String,
    val fullName: String,
    val password: String,
    val status: Status,
    val groupId: String
)

@Serializable
data class Teacher(
    val username: String,
    val fullName: String,
    val password: String
)