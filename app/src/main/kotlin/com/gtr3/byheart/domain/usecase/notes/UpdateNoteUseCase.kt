package com.gtr3.byheart.domain.usecase.notes

import com.gtr3.byheart.domain.repository.NotesRepository
import javax.inject.Inject

class UpdateNoteUseCase @Inject constructor(private val repository: NotesRepository) {
    suspend operator fun invoke(noteId: Long, title: String? = null, content: String? = null, folderName: String? = null, isPinned: Boolean? = null) =
        repository.updateNote(noteId, title, content, folderName, isPinned)
}
