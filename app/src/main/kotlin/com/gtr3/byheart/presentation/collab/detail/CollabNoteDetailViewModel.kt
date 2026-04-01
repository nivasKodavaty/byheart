package com.gtr3.byheart.presentation.collab.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtr3.byheart.core.util.Result
import com.gtr3.byheart.domain.model.CollabUpdateResult
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
class CollabNoteDetailViewModel @Inject constructor(
    private val collabRepository: CollabRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CollabNoteDetailState())
    val state = _state.asStateFlow()

    private val _effect = Channel<CollabNoteDetailEffect>()
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: CollabNoteDetailIntent) {
        when (intent) {
            is CollabNoteDetailIntent.Load                  -> loadNote(intent.shareCode)
            is CollabNoteDetailIntent.TitleChanged          -> _state.update { it.copy(editedTitle = intent.value, hasUnsavedChanges = true) }
            is CollabNoteDetailIntent.EditedContentChanged  -> _state.update {
                it.copy(editedContent = intent.html, hasUnsavedChanges = intent.html != (it.note?.content ?: ""))
            }
            is CollabNoteDetailIntent.SaveContent           -> saveContent(intent.html)
            CollabNoteDetailIntent.PollForUpdates           -> pollForUpdates()
            CollabNoteDetailIntent.AcceptConflict           -> {
                _state.update { it.copy(conflictContent = null, conflictVersion = null, hasUnsavedChanges = false) }
            }
            CollabNoteDetailIntent.ShowShareCode            -> _state.update { it.copy(showShareCodeSheet = true) }
            CollabNoteDetailIntent.DismissShareCode         -> _state.update { it.copy(showShareCodeSheet = false) }
            CollabNoteDetailIntent.LeaveNote                -> leaveNote()
            is CollabNoteDetailIntent.AiInputChanged        -> _state.update { it.copy(aiInput = intent.value) }
            CollabNoteDetailIntent.SendAiMessage            -> {
                if (_state.value.selectionText.isNotEmpty()) sendSelectionEdit()
                else sendChatMessage()
            }
            is CollabNoteDetailIntent.TextSelected          -> _state.update {
                it.copy(selectionText = intent.text, selectionStart = intent.start, selectionEnd = intent.end, aiInput = "")
            }
            CollabNoteDetailIntent.ApplySelectionReplacement -> _state.update {
                it.copy(selectionText = "", aiInput = "", selectionReplacement = null)
            }
            CollabNoteDetailIntent.DiscardSelectionReplacement -> _state.update { it.copy(selectionReplacement = null) }
            CollabNoteDetailIntent.DismissSelectionEdit     -> _state.update {
                it.copy(selectionText = "", aiInput = "", selectionReplacement = null, isSelectionLoading = false)
            }
            CollabNoteDetailIntent.ReminderToggled          -> _state.update { it.copy(reminderEnabled = !it.reminderEnabled) }
            is CollabNoteDetailIntent.ReminderTitleChanged  -> _state.update { it.copy(reminderTitle = intent.value) }
            is CollabNoteDetailIntent.ReminderTimeChanged   -> _state.update { it.copy(reminderTimeMillis = intent.millis) }
            is CollabNoteDetailIntent.ReminderDayToggled    -> _state.update {
                val days = it.reminderDays.toMutableSet()
                if (!days.add(intent.dayIndex)) days.remove(intent.dayIndex)
                it.copy(reminderDays = days)
            }
            CollabNoteDetailIntent.NavigateBack             -> viewModelScope.launch { _effect.send(CollabNoteDetailEffect.NavigateBack) }
        }
    }

    private fun loadNote(shareCode: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = collabRepository.getCollabNote(shareCode)) {
                is Result.Success -> _state.update {
                    it.copy(
                        isLoading      = false,
                        note           = result.data,
                        editedTitle    = result.data.title,
                        editedContent  = result.data.content ?: "",
                        chatMessages   = result.data.messages
                    )
                }
                is Result.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                else -> _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun pollForUpdates() {
        val shareCode = _state.value.note?.shareCode ?: return
        if (_state.value.hasUnsavedChanges) return
        viewModelScope.launch {
            when (val result = collabRepository.getCollabNote(shareCode)) {
                is Result.Success -> {
                    val fresh = result.data
                    val current = _state.value.note
                    if (fresh.version != current?.version) {
                        _state.update {
                            it.copy(
                                note          = fresh,
                                editedTitle   = fresh.title,
                                editedContent = fresh.content ?: "",
                                chatMessages  = fresh.messages,
                                aiContentVersion = it.aiContentVersion + 1
                            )
                        }
                        _effect.send(CollabNoteDetailEffect.ReloadEditor)
                    }
                }
                else -> { /* silently ignore poll failures */ }
            }
        }
    }

    private fun saveContent(html: String) {
        val note = _state.value.note ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            when (val result = collabRepository.updateCollabNote(
                shareCode = note.shareCode,
                title     = _state.value.editedTitle.takeIf { it.isNotBlank() },
                content   = html,
                version   = note.version
            )) {
                is CollabUpdateResult.Success -> {
                    _state.update {
                        it.copy(
                            isSaving         = false,
                            note             = result.note,
                            editedTitle      = result.note.title,
                            editedContent    = result.note.content ?: "",
                            hasUnsavedChanges = false
                        )
                    }
                    scheduleReminderIfNeeded(result.note.id, result.note.title)
                }
                is CollabUpdateResult.Conflict -> {
                    _state.update {
                        it.copy(
                            isSaving        = false,
                            conflictContent = result.latestContent,
                            conflictVersion = result.latestVersion
                        )
                    }
                    _effect.send(
                        CollabNoteDetailEffect.ConflictDetected(result.latestContent, result.latestVersion)
                    )
                }
                is CollabUpdateResult.Error -> {
                    _state.update { it.copy(isSaving = false) }
                    _effect.send(CollabNoteDetailEffect.ShowError(result.message))
                }
            }
        }
    }

    private fun sendChatMessage() {
        val note = _state.value.note ?: return
        val message = _state.value.aiInput.trim()
        if (message.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isAiLoading = true, aiInput = "", chatError = null) }
            when (val result = collabRepository.chatOnCollabNote(note.shareCode, message)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isAiLoading      = false,
                            note             = result.data,
                            chatMessages     = result.data.messages,
                            aiContentVersion = it.aiContentVersion + 1
                        )
                    }
                    _effect.send(CollabNoteDetailEffect.ReloadEditor)
                }
                is Result.Error -> _state.update { it.copy(isAiLoading = false, chatError = result.message) }
                else -> _state.update { it.copy(isAiLoading = false) }
            }
        }
    }

    private fun sendSelectionEdit() {
        val note = _state.value.note ?: return
        val instruction = _state.value.aiInput.trim()
        if (instruction.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSelectionLoading = true, chatError = null) }
            when (val result = collabRepository.refineSelection(note.shareCode, _state.value.selectionText, instruction)) {
                is Result.Success -> _state.update { it.copy(isSelectionLoading = false, selectionReplacement = result.data, aiInput = "") }
                is Result.Error   -> _state.update { it.copy(isSelectionLoading = false, chatError = result.message) }
                else -> _state.update { it.copy(isSelectionLoading = false) }
            }
        }
    }

    private fun leaveNote() {
        val note = _state.value.note ?: return
        viewModelScope.launch {
            collabRepository.leaveCollabNote(note.shareCode)
            _effect.send(CollabNoteDetailEffect.NoteLeft)
        }
    }

    private fun scheduleReminderIfNeeded(noteId: Long, noteTitle: String) {
        val s = _state.value
        if (!s.reminderEnabled) return
        val isRepeating = s.reminderDays.isNotEmpty()
        val now = System.currentTimeMillis()
        if (!isRepeating && s.reminderTimeMillis <= now) return
        viewModelScope.launch {
            _effect.send(
                CollabNoteDetailEffect.ScheduleReminder(
                    noteId          = noteId,
                    noteTitle       = noteTitle,
                    reminderTitle   = s.reminderTitle.ifBlank { noteTitle },
                    triggerAtMillis = s.reminderTimeMillis,
                    reminderDays    = s.reminderDays
                )
            )
        }
    }
}
