package com.gtr3.byheart.presentation.notes.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtr3.byheart.domain.usecase.notes.DeleteNoteUseCase
import com.gtr3.byheart.domain.usecase.notes.GetNotesUseCase
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
    private val deleteNoteUseCase: DeleteNoteUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(NotesListState())
    val state = _state.asStateFlow()

    private val _effect = Channel<NotesListEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        // Continuously observe Room for live updates
        viewModelScope.launch {
            getNotesUseCase().collect { notes ->
                _state.update { it.copy(notes = notes, isLoading = false) }
            }
        }
        // Sync latest from network on launch (Room flow auto-updates after upsert)
        viewModelScope.launch { getNotesUseCase.refresh() }
    }

    fun onIntent(intent: NotesListIntent) {
        when (intent) {
            is NotesListIntent.DeleteNote -> deleteNote(intent.id)
            is NotesListIntent.OpenNote   -> viewModelScope.launch {
                _effect.send(NotesListEffect.NavigateToDetail(intent.id))
            }
            NotesListIntent.CreateNote    -> viewModelScope.launch {
                _effect.send(NotesListEffect.NavigateToCreate)
            }
        }
    }

    private fun deleteNote(id: Long) = viewModelScope.launch {
        deleteNoteUseCase(id)
        // Room flow emits the updated list automatically
    }
}
