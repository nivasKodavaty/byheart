package com.gtr3.byheart.domain.usecase.notes

import com.gtr3.byheart.domain.repository.NotesRepository
import javax.inject.Inject

class GetNotesUseCase @Inject constructor(private val repository: NotesRepository) {
    suspend operator fun invoke() = repository.getAllNotes()
}
