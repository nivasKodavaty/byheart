package com.gtr3.byheart.data.repository

import com.google.gson.Gson
import com.gtr3.byheart.core.util.Result
import com.gtr3.byheart.data.remote.api.CollabApi
import com.gtr3.byheart.data.remote.dto.*
import com.gtr3.byheart.domain.model.CollabNote
import com.gtr3.byheart.domain.model.CollabUpdateResult
import com.gtr3.byheart.domain.model.Message
import com.gtr3.byheart.domain.repository.CollabRepository
import retrofit2.HttpException
import javax.inject.Inject

class CollabRepositoryImpl @Inject constructor(
    private val api: CollabApi,
    private val gson: Gson
) : CollabRepository {

    override suspend fun getAllCollabNotes(): Result<List<CollabNote>> =
        runCatching {
            Result.Success(api.getAllCollabNotes().map { it.toDomain() })
        }.getOrElse { Result.Error(it.message ?: "Failed to load collab notes") }

    override suspend fun getCollabNote(shareCode: String): Result<CollabNote> =
        runCatching {
            Result.Success(api.getCollabNote(shareCode).toDomain())
        }.getOrElse { Result.Error(it.message ?: "Failed to load note") }

    override suspend fun createCollabNote(title: String, content: String?, useAi: Boolean): Result<CollabNote> =
        runCatching {
            val dto = api.createCollabNote(CreateCollabNoteRequest(title, content, useAi))
            Result.Success(dto.toDomain())
        }.getOrElse { Result.Error(it.message ?: "Failed to create note") }

    override suspend fun joinCollabNote(shareCode: String): Result<CollabNote> =
        runCatching {
            val dto = api.joinCollabNote(JoinCollabNoteRequest(shareCode.trim().uppercase()))
            Result.Success(dto.toDomain())
        }.getOrElse { Result.Error(it.message ?: "Invalid code or note not found") }

    override suspend fun updateCollabNote(
        shareCode: String,
        title: String?,
        content: String?,
        version: Long
    ): CollabUpdateResult {
        return try {
            val response = api.updateCollabNote(shareCode, UpdateCollabNoteRequest(title, content, version))
            if (response.isSuccessful) {
                CollabUpdateResult.Success(response.body()!!.toDomain())
            } else if (response.code() == 409) {
                val errorJson = response.errorBody()?.string() ?: "{}"
                val conflict = gson.fromJson(errorJson, ConflictResponseDto::class.java)
                CollabUpdateResult.Conflict(
                    latestTitle   = conflict.currentTitle,
                    latestContent = conflict.currentContent,
                    latestVersion = conflict.currentVersion
                )
            } else {
                CollabUpdateResult.Error("Save failed (${response.code()})")
            }
        } catch (e: HttpException) {
            CollabUpdateResult.Error(e.message ?: "Save failed")
        } catch (e: Exception) {
            CollabUpdateResult.Error(e.message ?: "Save failed")
        }
    }

    override suspend fun chatOnCollabNote(shareCode: String, message: String): Result<CollabNote> =
        runCatching {
            val dto = api.chatOnCollabNote(shareCode, CollabChatRequest(message))
            Result.Success(dto.toDomain())
        }.getOrElse { Result.Error(it.message ?: "Chat failed") }

    override suspend fun refineSelection(
        shareCode: String,
        selectedText: String,
        instruction: String
    ): Result<String> =
        runCatching {
            val response = api.refineSelection(shareCode, CollabRefineSelectionRequest(selectedText, instruction))
            Result.Success(response.replacement)
        }.getOrElse { Result.Error(it.message ?: "Refinement failed") }

    override suspend fun leaveCollabNote(shareCode: String): Result<Unit> =
        runCatching {
            api.leaveCollabNote(shareCode)
            Result.Success(Unit)
        }.getOrElse { Result.Error(it.message ?: "Failed to leave note") }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private fun CollabNoteDto.toDomain() = CollabNote(
        id               = id,
        shareCode        = shareCode,
        title            = title,
        content          = content,
        updatedAt        = updatedAt,
        lastEditedBy     = lastEditedBy,
        version          = version,
        participantCount = participantCount,
        messages         = messages.map { Message(it.id, it.role, it.message, it.createdAt) }
    )
}
