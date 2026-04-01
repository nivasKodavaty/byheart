package com.gtr3.byheart.domain.usecase.notes

import com.gtr3.byheart.domain.repository.NotesRepository
import javax.inject.Inject

class GetNoteByIdUseCase @Inject constructor(private val repository: NotesRepository) {
    suspend operator fun invoke(id: Long) = repository.getNoteById(id)
}
