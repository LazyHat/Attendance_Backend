package ru.lazyhat.models

data class Group(
    val id: String,
    val lessonsList: Set<UInt>
)