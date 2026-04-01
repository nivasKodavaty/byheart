package com.gtr3.byheart.domain.repository

import com.gtr3.byheart.core.util.Result
import com.gtr3.byheart.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    fun getAllNotes(): Flow<List<Note>>
    fun getNoteById(id: Long): Flow<Note?>
    suspend fun refreshNotes(): Result<Unit>
    suspend fun createNote(title: String, content: String? = null, useAi: Boolean = false): Result<Note>
    suspend fun updateNote(noteId: Long, title: String?, content: String?): Result<Note>
    suspend fun deleteNote(id: Long): Result<Unit>
}
