package com.gtr3.byheart.domain.repository

import com.gtr3.byheart.core.util.Result
import com.gtr3.byheart.domain.model.CollabNote
import com.gtr3.byheart.domain.model.CollabParticipant
import com.gtr3.byheart.domain.model.CollabUpdateResult

interface CollabRepository {
    suspend fun getAllCollabNotes(): Result<List<CollabNote>>
    suspend fun getCollabNote(shareCode: String): Result<CollabNote>
    suspend fun createCollabNote(title: String, content: String?, useAi: Boolean): Result<CollabNote>
    suspend fun joinCollabNote(shareCode: String): Result<CollabNote>
    suspend fun updateCollabNote(shareCode: String, title: String?, content: String?, version: Long): CollabUpdateResult
    suspend fun chatOnCollabNote(shareCode: String, message: String): Result<CollabNote>
    suspend fun refineSelection(shareCode: String, selectedText: String, instruction: String): Result<String>
    suspend fun getParticipants(shareCode: String): Result<List<CollabParticipant>>
    suspend fun leaveCollabNote(shareCode: String): Result<Unit>
}
