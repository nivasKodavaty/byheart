package com.gtr3.byheart.data.remote.dto

data class CreateNoteRequest(val title: String, val content: String? = null, val useAi: Boolean = false)

data class UpdateNoteRequest(
    val noteId: Long,
    val title: String?,
    val content: String?
)

data class MessageDto(
    val id: Long,
    val role: String,
    val message: String,
    val createdAt: String
)

data class NoteDto(
    val id: Long,
    val title: String,
    val content: String?,
    val updatedAt: String,
    val messages: List<MessageDto> = emptyList()
)
