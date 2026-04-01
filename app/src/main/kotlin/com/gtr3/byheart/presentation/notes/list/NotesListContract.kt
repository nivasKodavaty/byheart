package com.gtr3.byheart.presentation.notes.list

import com.gtr3.byheart.domain.model.Note

data class NotesListState(
    val isLoading: Boolean = false,
    val notes: List<Note> = emptyList(),
    val error: String? = null
)

sealed class NotesListIntent {
    data object LoadNotes : NotesListIntent()
    data class DeleteNote(val id: Long) : NotesListIntent()
    data class OpenNote(val id: Long) : NotesListIntent()
    data object CreateNote : NotesListIntent()
}

sealed class NotesListEffect {
    data class NavigateToDetail(val id: Long) : NotesListEffect()
    data object NavigateToCreate : NotesListEffect()
}
