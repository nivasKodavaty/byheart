package com.gtr3.byheart.presentation.collab.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtr3.byheart.core.util.Result
import com.gtr3.byheart.domain.repository.CollabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollabNotesListViewModel @Inject constructor(
    private val collabRepository: CollabRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CollabNotesListState())
    val state = _state.asStateFlow()

    private val _effect = Channel<CollabNotesListEffect>()
    val effect = _effect.receiveAsFlow()

    init { onIntent(CollabNotesListIntent.LoadNotes) }

    fun onIntent(intent: CollabNotesListIntent) {
        when (intent) {
            CollabNotesListIntent.LoadNotes             -> loadNotes()
            CollabNotesListIntent.ShowCreateSheet       -> _state.update { it.copy(showCreateSheet = true, createTitle = "", createUseAi = false) }
            CollabNotesListIntent.DismissCreateSheet    -> _state.update { it.copy(showCreateSheet = false) }
            is CollabNotesListIntent.CreateTitleChanged -> _state.update { it.copy(createTitle = intent.value) }
            is CollabNotesListIntent.CreateUseAiToggled -> _state.update { it.copy(createUseAi = intent.enabled) }
            CollabNotesListIntent.CreateNote            -> createNote()
            CollabNotesListIntent.ShowJoinSheet         -> _state.update { it.copy(showJoinSheet = true, joinCode = "", joinError = null) }
            CollabNotesListIntent.DismissJoinSheet      -> _state.update { it.copy(showJoinSheet = false, joinError = null) }
            is CollabNotesListIntent.JoinCodeChanged    -> _state.update { it.copy(joinCode = intent.value, joinError = null) }
            CollabNotesListIntent.JoinNote              -> joinNote()
        }
    }

    private fun loadNotes() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = collabRepository.getAllCollabNotes()) {
                is Result.Success -> _state.update { it.copy(isLoading = false, notes = result.data) }
                is Result.Error   -> _state.update { it.copy(isLoading = false, error = result.message) }
                else -> _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun createNote() {
        val title = _state.value.createTitle.trim()
        if (title.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true) }
            when (val result = collabRepository.createCollabNote(title, null, _state.value.createUseAi)) {
                is Result.Success -> {
                    _state.update { it.copy(isCreating = false, showCreateSheet = false) }
                    _effect.send(CollabNotesListEffect.NavigateToCreate(result.data.shareCode))
                }
                is Result.Error -> _state.update { it.copy(isCreating = false, error = result.message) }
                else -> _state.update { it.copy(isCreating = false) }
            }
        }
    }

    private fun joinNote() {
        val code = _state.value.joinCode.trim().uppercase()
        if (code.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isJoining = true, joinError = null) }
            when (val result = collabRepository.joinCollabNote(code)) {
                is Result.Success -> {
                    _state.update { it.copy(isJoining = false, showJoinSheet = false) }
                    _effect.send(CollabNotesListEffect.NavigateToDetail(result.data.shareCode))
                }
                is Result.Error -> _state.update { it.copy(isJoining = false, joinError = result.message) }
                else -> _state.update { it.copy(isJoining = false) }
            }
        }
    }
}
