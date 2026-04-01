package com.gtr3.byheart.presentation.notes.detail

import com.gtr3.byheart.domain.model.Note

data class NoteDetailState(
    val isLoading: Boolean = false,
    val note: Note? = null,
    val titleInput: String = "",
    val contentInput: String = "",
    val useAi: Boolean = false,
    val error: String? = null
)

sealed class NoteDetailIntent {
    data class LoadNote(val id: Long) : NoteDetailIntent()
    data class TitleChanged(val value: String) : NoteDetailIntent()
    data class ContentChanged(val value: String) : NoteDetailIntent()
    data class UseAiToggled(val enabled: Boolean) : NoteDetailIntent()
    data object CreateNote : NoteDetailIntent()
    data object NavigateBack : NoteDetailIntent()
}

sealed class NoteDetailEffect {
    data object NavigateBack : NoteDetailEffect()
    data class NavigateToDetail(val id: Long) : NoteDetailEffect()
    data class ShowError(val message: String) : NoteDetailEffect()
}
