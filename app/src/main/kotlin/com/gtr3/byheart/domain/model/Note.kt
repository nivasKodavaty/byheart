package com.gtr3.byheart.domain.model

data class Note(
    val id: Long,
    val title: String,
    val content: String?,
    val updatedAt: String,
    val messages: List<Message> = emptyList()
)

data class Message(
    val id: Long,
    val role: String,
    val message: String,
    val createdAt: String
)
