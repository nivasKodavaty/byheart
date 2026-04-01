package com.gtr3.byheart.data.repository

import com.gtr3.byheart.core.util.Result
import com.gtr3.byheart.data.local.dao.NoteDao
import com.gtr3.byheart.data.local.entity.NoteEntity
import com.gtr3.byheart.data.remote.api.NotesApi
import com.gtr3.byheart.data.remote.dto.ChatRequest
import com.gtr3.byheart.data.remote.dto.CreateNoteRequest
import com.gtr3.byheart.data.remote.dto.NoteDto
import com.gtr3.byheart.data.remote.dto.RefineSelectionRequest
import com.gtr3.byheart.data.remote.dto.UpdateNoteRequest
import com.gtr3.byheart.domain.model.Message
import com.gtr3.byheart.domain.model.Note
import com.gtr3.byheart.domain.repository.NotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NotesRepositoryImpl @Inject constructor(
    private val api: NotesApi,
    private val noteDao: NoteDao
) : NotesRepository {

    override fun getAllNotes(): Flow<List<Note>> =
        noteDao.getAllNotes().map { list -> list.map { it.toDomain() } }

    override fun getNoteById(id: Long): Flow<Note?> =
        noteDao.getNoteByIdFlow(id).map { it?.toDomain() }

    override fun getDistinctFolders(): Flow<List<String>> =
        noteDao.getDistinctFolders()

    override suspend fun refreshNotes(): Result<Unit> =
        runCatching {
            val notes = api.getAllNotes()
            noteDao.upsertNotes(notes.map { it.toEntity() })
            Result.Success(Unit)
        }.getOrElse { Result.Error(it.message ?: "Failed to sync notes") }

    override suspend fun fetchNoteWithMessages(id: Long): Result<Note> =
        runCatching {
            val note = api.getNoteById(id)
            noteDao.upsertNote(note.toEntity())
            Result.Success(note.toDomainWithMessages())
        }.getOrElse { Result.Error(it.message ?: "Failed to fetch note") }

    override suspend fun createNote(title: String, content: String?, useAi: Boolean, folderName: String?): Result<Note> =
        runCatching {
            val note = api.createNote(CreateNoteRequest(title, content, useAi, folderName))
            noteDao.upsertNote(note.toEntity())
            Result.Success(note.toDomainWithMessages())
        }.getOrElse { Result.Error(it.message ?: "Failed to create note") }

    override suspend fun updateNote(noteId: Long, title: String?, content: String?, folderName: String?, isPinned: Boolean?): Result<Note> =
        runCatching {
            val note = api.updateNote(UpdateNoteRequest(noteId, title, content, folderName, isPinned))
            noteDao.upsertNote(note.toEntity())
            Result.Success(note.toDomainWithMessages())
        }.getOrElse { Result.Error(it.message ?: "Failed to update note") }

    override suspend fun pinNote(noteId: Long): Result<Note> =
        runCatching {
            val note = api.pinNote(noteId)
            noteDao.upsertNote(note.toEntity())
            Result.Success(note.toDomainWithMessages())
        }.getOrElse { Result.Error(it.message ?: "Failed to pin note") }

    override suspend fun chatOnNote(noteId: Long, message: String): Result<Note> =
        runCatching {
            val note = api.chatOnNote(noteId, ChatRequest(message))
            noteDao.upsertNote(note.toEntity())
            Result.Success(note.toDomainWithMessages())
        }.getOrElse { Result.Error(it.message ?: "Chat failed") }

    override suspend fun refineSelection(noteId: Long, selectedText: String, instruction: String): Result<String> =
        runCatching {
            val response = api.refineSelection(noteId, RefineSelectionRequest(selectedText, instruction))
            Result.Success(response.replacement)
        }.getOrElse { Result.Error(it.message ?: "Failed to refine selection") }

    override suspend fun deleteNote(id: Long): Result<Unit> =
        runCatching {
            noteDao.deleteNoteById(id)
            api.deleteNote(id)
            Result.Success(Unit)
        }.getOrElse { Result.Error(it.message ?: "Failed to delete note") }

    private fun NoteDto.toEntity() = NoteEntity(id, title, content, updatedAt, isPinned, folderName)

    private fun NoteEntity.toDomain() = Note(id = id, title = title, content = content, updatedAt = updatedAt, isPinned = isPinned, folderName = folderName)

    private fun NoteDto.toDomainWithMessages() = Note(
        id = id,
        title = title,
        content = content,
        updatedAt = updatedAt,
        isPinned = isPinned,
        folderName = folderName,
        messages = messages.map { Message(it.id, it.role, it.message, it.createdAt) }
    )
}
