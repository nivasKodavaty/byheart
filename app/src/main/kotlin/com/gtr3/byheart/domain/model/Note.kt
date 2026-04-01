package com.gtr3.byheart.domain.model

data class Note(
    val id: Long,
    val title: String,
    val content: String?,
    val updatedAt: String,
    val isPinned: Boolean = false,
    val folderName: String? = null,
    val messages: List<Message> = emptyList()
)

data class Message(
    val id: Long,
    val role: String,
    val message: String,
    val createdAt: String
)
