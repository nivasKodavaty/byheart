package com.gtr3.byheart.presentation.notes.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtr3.byheart.domain.usecase.notes.DeleteNoteUseCase
import com.gtr3.byheart.domain.usecase.notes.GetNotesUseCase
import com.gtr3.byheart.domain.usecase.notes.PinNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesListViewModel @Inject constructor(
    private val getNotesUseCase: GetNotesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val pinNoteUseCase: PinNoteUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(NotesListState())
    val state = _state.asStateFlow()

    private val _effect = Channel<NotesListEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            getNotesUseCase().collect { notes ->
                _state.update { it.copy(notes = notes, isLoading = false) }
            }
        }
        viewModelScope.launch {
            getNotesUseCase.getFolders().collect { folders ->
                _state.update { it.copy(folders = folders) }
            }
        }
        viewModelScope.launch { getNotesUseCase.refresh() }
    }

    fun onIntent(intent: NotesListIntent) {
        when (intent) {
            is NotesListIntent.DeleteNote     -> deleteNote(intent.id)
            is NotesListIntent.PinNote        -> pinNote(intent.id)
            is NotesListIntent.SearchChanged  -> _state.update { it.copy(searchQuery = intent.query) }
            is NotesListIntent.SelectFolder   -> _state.update { it.copy(selectedFolder = intent.folder, searchQuery = "") }
            is NotesListIntent.ToggleFolderCollapse -> _state.update { s ->
                val newCollapsed = if (intent.folder in s.collapsedFolders)
                    s.collapsedFolders - intent.folder
                else
                    s.collapsedFolders + intent.folder
                s.copy(collapsedFolders = newCollapsed)
            }
            NotesListIntent.Refresh           -> refresh()
            is NotesListIntent.OpenNote       -> viewModelScope.launch {
                _effect.send(NotesListEffect.NavigateToDetail(intent.id))
            }
            NotesListIntent.CreateNote        -> viewModelScope.launch {
                _effect.send(NotesListEffect.NavigateToCreate)
            }
        }
    }

    private fun refresh() = viewModelScope.launch {
        _state.update { it.copy(isRefreshing = true) }
        getNotesUseCase.refresh()
        _state.update { it.copy(isRefreshing = false) }
    }

    private fun pinNote(id: Long) = viewModelScope.launch {
        pinNoteUseCase(id)
    }

    private fun deleteNote(id: Long) = viewModelScope.launch {
        deleteNoteUseCase(id)
    }
}
