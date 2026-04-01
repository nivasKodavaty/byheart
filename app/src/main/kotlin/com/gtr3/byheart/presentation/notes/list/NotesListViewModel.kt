package com.gtr3.byheart.presentation.notes.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtr3.byheart.core.util.Result
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

    init { onIntent(NotesListIntent.LoadNotes) }

    fun onIntent(intent: NotesListIntent) {
        when (intent) {
            NotesListIntent.LoadNotes        -> loadNotes()
            is NotesListIntent.DeleteNote    -> deleteNote(intent.id)
            is NotesListIntent.OpenNote      -> viewModelScope.launch {
                _effect.send(NotesListEffect.NavigateToDetail(intent.id))
            }
            NotesListIntent.CreateNote       -> viewModelScope.launch {
                _effect.send(NotesListEffect.NavigateToCreate)
            }
        }
    }

    private fun loadNotes() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        when (val result = getNotesUseCase()) {
            is Result.Success -> _state.update { it.copy(notes = result.data) }
            is Result.Error   -> _state.update { it.copy(error = result.message) }
            else              -> Unit
        }
        _state.update { it.copy(isLoading = false) }
    }

    private fun deleteNote(id: Long) = viewModelScope.launch {
        deleteNoteUseCase(id)
        loadNotes()
    }
}
