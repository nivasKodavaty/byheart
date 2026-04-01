package com.gtr3.byheart.presentation.notes.list

import com.gtr3.byheart.domain.model.Note

data class NotesListState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val notes: List<Note> = emptyList(),
    val folders: List<String> = emptyList(),
    val selectedFolder: String? = null,   // null = All Notes
    val searchQuery: String = "",
    val error: String? = null
) {
    // Apply folder filter first, then search
    val filtered: List<Note>
        get() {
            val byFolder = if (selectedFolder == null) notes
                           else notes.filter { it.folderName == selectedFolder }
            return if (searchQuery.isBlank()) byFolder
                   else byFolder.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }

    val pinned: List<Note>   get() = filtered.filter { it.isPinned }
    val nonPinned: List<Note> get() = filtered.filter { !it.isPinned }
    val unfiled: List<Note>  get() = filtered.filter { !it.isPinned && it.folderName == null }

    fun notesInFolder(folder: String): List<Note> =
        filtered.filter { !it.isPinned && it.folderName == folder }

    // Counts used by the drawer
    val allNotesCount: Int get() = notes.size
    fun folderCount(folder: String): Int = notes.count { it.folderName == folder }
}

sealed class NotesListIntent {
    data class DeleteNote(val id: Long) : NotesListIntent()
    data class OpenNote(val id: Long) : NotesListIntent()
    data class PinNote(val id: Long) : NotesListIntent()
    data class SearchChanged(val query: String) : NotesListIntent()
    data class SelectFolder(val folder: String?) : NotesListIntent()
    data object Refresh : NotesListIntent()
    data object CreateNote : NotesListIntent()
}

sealed class NotesListEffect {
    data class NavigateToDetail(val id: Long) : NotesListEffect()
    data object NavigateToCreate : NotesListEffect()
}
