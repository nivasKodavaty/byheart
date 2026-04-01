package com.gtr3.byheart.data.remote.dto

data class CreateNoteRequest(
    val title: String,
    val content: String? = null,
    val useAi: Boolean = false,
    val folderName: String? = null
)

data class UpdateNoteRequest(
    val noteId: Long,
    val title: String? = null,
    val content: String? = null,
    val folderName: String? = null,
    val isPinned: Boolean? = null
)

data class ChatRequest(val message: String)

data class RefineSelectionRequest(val selectedText: String, val instruction: String)

data class RefineSelectionResponse(val replacement: String)

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
    val isPinned: Boolean = false,
    val folderName: String? = null,
    val messages: List<MessageDto> = emptyList()
)
