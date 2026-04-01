package com.gtr3.byheart.domain.usecase.notes

import com.gtr3.byheart.domain.model.Note
import com.gtr3.byheart.domain.repository.NotesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNoteByIdUseCase @Inject constructor(private val repository: NotesRepository) {
    operator fun invoke(id: Long): Flow<Note?> = repository.getNoteById(id)
    suspend fun fetch(id: Long) = repository.fetchNoteWithMessages(id)
}
