package com.gtr3.byheart.domain.repository

import com.gtr3.byheart.core.util.Result
import com.gtr3.byheart.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    fun getAllNotes(): Flow<List<Note>>
    fun getNoteById(id: Long): Flow<Note?>
    fun getDistinctFolders(): Flow<List<String>>
    suspend fun refreshNotes(): Result<Unit>
    suspend fun fetchNoteWithMessages(id: Long): Result<Note>
    suspend fun createNote(title: String, content: String? = null, useAi: Boolean = false, folderName: String? = null): Result<Note>
    suspend fun updateNote(noteId: Long, title: String? = null, content: String? = null, folderName: String? = null, isPinned: Boolean? = null): Result<Note>
    suspend fun pinNote(noteId: Long): Result<Note>
    suspend fun chatOnNote(noteId: Long, message: String): Result<Note>
    suspend fun refineSelection(noteId: Long, selectedText: String, instruction: String): Result<String>
    suspend fun deleteNote(id: Long): Result<Unit>
}
