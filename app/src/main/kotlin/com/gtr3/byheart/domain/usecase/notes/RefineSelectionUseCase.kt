package com.gtr3.byheart.domain.usecase.notes

import com.gtr3.byheart.domain.repository.NotesRepository
import javax.inject.Inject

class RefineSelectionUseCase @Inject constructor(private val repository: NotesRepository) {
    suspend operator fun invoke(noteId: Long, selectedText: String, instruction: String) =
        repository.refineSelection(noteId, selectedText, instruction)
}
