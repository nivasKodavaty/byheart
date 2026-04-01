package com.gtr3.byheart.domain.repository

import com.gtr3.byheart.core.util.Result
import com.gtr3.byheart.domain.model.Note

interface NotesRepository {
    suspend fun getAllNotes(): Result<List<Note>>
    suspend fun getNoteById(id: Long): Result<Note>
    suspend fun createNote(title: String): Result<Note>
    suspend fun updateNote(noteId: Long, title: String?, content: String?): Result<Note>
    suspend fun deleteNote(id: Long): Result<Unit>
}
