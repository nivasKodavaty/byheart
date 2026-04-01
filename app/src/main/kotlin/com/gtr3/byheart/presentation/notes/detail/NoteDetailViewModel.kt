package com.gtr3.byheart.presentation.notes.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtr3.byheart.core.util.Result
import com.gtr3.byheart.domain.usecase.notes.CreateNoteUseCase
import com.gtr3.byheart.domain.usecase.notes.GetNoteByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val createNoteUseCase: CreateNoteUseCase,
    private val getNoteByIdUseCase: GetNoteByIdUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(NoteDetailState())
    val state = _state.asStateFlow()

    private val _effect = Channel<NoteDetailEffect>()
    val effect = _effect.receiveAsFlow()

    private var loadJob: Job? = null

    fun onIntent(intent: NoteDetailIntent) {
        when (intent) {
            is NoteDetailIntent.LoadNote       -> loadNote(intent.id)
            is NoteDetailIntent.TitleChanged   -> _state.update { it.copy(titleInput = intent.value) }
            is NoteDetailIntent.ContentChanged -> _state.update { it.copy(contentInput = intent.value) }
            is NoteDetailIntent.UseAiToggled   -> _state.update { it.copy(useAi = intent.enabled) }
            NoteDetailIntent.CreateNote        -> createNote()
            NoteDetailIntent.NavigateBack      -> viewModelScope.launch {
                _effect.send(NoteDetailEffect.NavigateBack)
            }
        }
    }

    private fun loadNote(id: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getNoteByIdUseCase(id).collect { note ->
                _state.update { it.copy(note = note, isLoading = false) }
            }
        }
    }

    private fun createNote() = viewModelScope.launch {
        val title = _state.value.titleInput.trim()
        if (title.isBlank()) {
            _state.update { it.copy(error = "Title cannot be empty") }
            return@launch
        }
        val useAi = _state.value.useAi
        val content = if (useAi) null else _state.value.contentInput.trim().ifBlank { null }
        _state.update { it.copy(isLoading = true, error = null) }
        when (val result = createNoteUseCase(title, content, useAi)) {
            is Result.Success -> _effect.send(NoteDetailEffect.NavigateToDetail(result.data.id))
            is Result.Error   -> _state.update { it.copy(error = result.message, isLoading = false) }
            else -> Unit
        }
    }
}
