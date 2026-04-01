package com.gtr3.byheart.domain.usecase.notes

import com.gtr3.byheart.domain.model.Note
import com.gtr3.byheart.domain.repository.NotesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNotesUseCase @Inject constructor(private val repository: NotesRepository) {
    operator fun invoke(): Flow<List<Note>> = repository.getAllNotes()
    suspend fun refresh() = repository.refreshNotes()
}
