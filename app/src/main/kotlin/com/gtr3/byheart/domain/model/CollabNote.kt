package com.gtr3.byheart.domain.model

data class CollabParticipant(
    val email: String,
    val displayName: String?,
    val joinedAt: String
)

data class CollabNote(
    val id: Long,
    val shareCode: String,
    val title: String,
    val content: String?,
    val updatedAt: String,
    val lastEditedBy: String?,
    val version: Long,
    val participantCount: Int,
    val messages: List<Message> = emptyList()
)

/** Result of an update attempt — Success, Conflict (stale version), or Error */
sealed class CollabUpdateResult {
    data class Success(val note: CollabNote) : CollabUpdateResult()
    data class Conflict(
        val latestTitle: String,
        val latestContent: String?,
        val latestVersion: Long
    ) : CollabUpdateResult()
    data class Error(val message: String) : CollabUpdateResult()
}
