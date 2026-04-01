package com.gtr3.byheart.domain.usecase.notes

import com.gtr3.byheart.domain.repository.NotesRepository
import javax.inject.Inject

class CreateNoteUseCase @Inject constructor(private val repository: NotesRepository) {
    suspend operator fun invoke(title: String, content: String? = null, useAi: Boolean = false, folderName: String? = null) =
        repository.createNote(title, content, useAi, folderName)
}
