package com.gtr3.byheart.data.repository

import com.gtr3.byheart.core.util.Result
import com.gtr3.byheart.data.local.dao.NoteDao
import com.gtr3.byheart.data.local.entity.NoteEntity
import com.gtr3.byheart.data.remote.api.NotesApi
import com.gtr3.byheart.data.remote.dto.CreateNoteRequest
import com.gtr3.byheart.data.remote.dto.NoteDto
import com.gtr3.byheart.data.remote.dto.UpdateNoteRequest
import com.gtr3.byheart.domain.model.Message
import com.gtr3.byheart.domain.model.Note
import com.gtr3.byheart.domain.repository.NotesRepository
import javax.inject.Inject

class NotesRepositoryImpl @Inject constructor(
    private val api: NotesApi,
    private val noteDao: NoteDao
) : NotesRepository {

    override suspend fun getAllNotes(): Result<List<Note>> =
        runCatching {
            val notes = api.getAllNotes()
            // Cache in Room
            noteDao.upsertNotes(notes.map { it.toEntity() })
            Result.Success(notes.map { it.toDomain() })
        }.getOrElse { Result.Error(it.message ?: "Failed to fetch notes") }

    override suspend fun getNoteById(id: Long): Result<Note> =
        runCatching {
            val note = api.getNoteById(id)
            noteDao.upsertNote(note.toEntity())
            Result.Success(note.toDomain())
        }.getOrElse { Result.Error(it.message ?: "Failed to fetch note") }

    override suspend fun createNote(title: String): Result<Note> =
        runCatching {
            val note = api.createNote(CreateNoteRequest(title))
            noteDao.upsertNote(note.toEntity())
            Result.Success(note.toDomain())
        }.getOrElse { Result.Error(it.message ?: "Failed to create note") }

    override suspend fun updateNote(noteId: Long, title: String?, content: String?): Result<Note> =
        runCatching {
            val note = api.updateNote(UpdateNoteRequest(noteId, title, content))
            noteDao.upsertNote(note.toEntity())
            Result.Success(note.toDomain())
        }.getOrElse { Result.Error(it.message ?: "Failed to update note") }

    override suspend fun deleteNote(id: Long): Result<Unit> =
        runCatching {
            api.deleteNote(id)
            noteDao.deleteNoteById(id)
            Result.Success(Unit)
        }.getOrElse { Result.Error(it.message ?: "Failed to delete note") }

    private fun NoteDto.toEntity() = NoteEntity(id, title, content, updatedAt)

    private fun NoteDto.toDomain() = Note(
        id = id,
        title = title,
        content = content,
        updatedAt = updatedAt,
        messages = messages.map { Message(it.id, it.role, it.message, it.createdAt) }
    )
}
