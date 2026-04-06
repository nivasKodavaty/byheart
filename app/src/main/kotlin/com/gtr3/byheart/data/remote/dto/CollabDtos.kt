package com.gtr3.byheart.data.remote.dto

data class CreateCollabNoteRequest(
    val title: String,
    val content: String? = null,
    val useAi: Boolean = false
)

data class JoinCollabNoteRequest(val shareCode: String)

data class UpdateCollabNoteRequest(
    val title: String? = null,
    val content: String? = null,
    val version: Long
)

data class CollabChatRequest(val message: String)

data class CollabRefineSelectionRequest(val selectedText: String, val instruction: String)

data class CollabNoteDto(
    val id: Long,
    val shareCode: String,
    val title: String,
    val content: String?,
    val updatedAt: String,
    val lastEditedBy: String?,
    val version: Long,
    val participantCount: Int,
    val messages: List<MessageDto> = emptyList()
)

data class ParticipantDto(
    val email: String,
    val displayName: String?,
    val joinedAt: String
)

/** Parsed from the 409 error body when a concurrent save wins the race */
data class ConflictResponseDto(
    val error: String = "conflict",
    val currentTitle: String,
    val currentContent: String?,
    val currentVersion: Long
)
