package com.gtr3.byheart.presentation.notes.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtr3.byheart.core.util.Result
import com.gtr3.byheart.domain.usecase.notes.ChatOnNoteUseCase
import com.gtr3.byheart.domain.usecase.notes.CreateNoteUseCase
import com.gtr3.byheart.domain.usecase.notes.GetNoteByIdUseCase
import com.gtr3.byheart.domain.usecase.notes.PinNoteUseCase
import com.gtr3.byheart.domain.usecase.notes.RefineSelectionUseCase
import com.gtr3.byheart.domain.usecase.notes.UpdateNoteUseCase
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
    private val getNoteByIdUseCase: GetNoteByIdUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val pinNoteUseCase: PinNoteUseCase,
    private val chatOnNoteUseCase: ChatOnNoteUseCase,
    private val refineSelectionUseCase: RefineSelectionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(NoteDetailState())
    val state = _state.asStateFlow()

    private val _effect = Channel<NoteDetailEffect>()
    val effect = _effect.receiveAsFlow()

    private var loadJob: Job? = null

    fun onIntent(intent: NoteDetailIntent) {
        when (intent) {
            is NoteDetailIntent.LoadNote              -> loadNote(intent.id)
            is NoteDetailIntent.TitleChanged          -> _state.update { it.copy(titleInput = intent.value) }
            is NoteDetailIntent.ContentChanged        -> _state.update { it.copy(contentInput = intent.value) }
            is NoteDetailIntent.UseAiToggled          -> _state.update { it.copy(useAi = intent.enabled) }
            is NoteDetailIntent.FolderInputChanged    -> _state.update { it.copy(folderInput = intent.value) }
            NoteDetailIntent.CreateNote               -> createNote()
            is NoteDetailIntent.EditedContentChanged  -> _state.update {
                it.copy(editedContent = intent.value, hasUnsavedChanges = intent.value != (it.note?.content ?: ""))
            }
            is NoteDetailIntent.SaveContent           -> saveContent(intent.html)
            NoteDetailIntent.TogglePin                -> togglePin()
            NoteDetailIntent.ShowFolderDialog         -> _state.update {
                it.copy(showFolderDialog = true, folderDialogInput = it.note?.folderName ?: "")
            }
            NoteDetailIntent.DismissFolderDialog      -> _state.update { it.copy(showFolderDialog = false) }
            is NoteDetailIntent.FolderDialogInputChanged -> _state.update { it.copy(folderDialogInput = intent.value) }
            NoteDetailIntent.SaveFolder               -> saveFolder()

            // Unified AI input
            is NoteDetailIntent.AiInputChanged        -> _state.update { it.copy(aiInput = intent.value) }
            NoteDetailIntent.SendAiMessage            -> {
                if (_state.value.selectionText.isNotEmpty()) sendSelectionEdit()
                else sendChatMessage()
            }

            // Selection
            is NoteDetailIntent.TextSelected          -> _state.update {
                it.copy(selectionText = intent.text, selectionStart = intent.start, selectionEnd = intent.end, aiInput = "")
            }
            NoteDetailIntent.ApplySelectionReplacement -> _state.update {
                it.copy(selectionText = "", aiInput = "", selectionReplacement = null)
            }
            NoteDetailIntent.DiscardSelectionReplacement -> _state.update {
                it.copy(selectionReplacement = null)
            }
            NoteDetailIntent.DismissSelectionEdit     -> _state.update {
                it.copy(selectionText = "", aiInput = "", selectionReplacement = null, isSelectionLoading = false)
            }

            // Reminder
            NoteDetailIntent.ReminderToggled          -> _state.update { it.copy(reminderEnabled = !it.reminderEnabled) }
            is NoteDetailIntent.ReminderTitleChanged  -> _state.update { it.copy(reminderTitle = intent.value) }
            is NoteDetailIntent.ReminderTimeChanged   -> _state.update { it.copy(reminderTimeMillis = intent.millis) }
            is NoteDetailIntent.ReminderDayToggled    -> _state.update {
                val days = it.reminderDays.toMutableSet()
                if (intent.dayIndex in days) days.remove(intent.dayIndex) else days.add(intent.dayIndex)
                it.copy(reminderDays = days)
            }

            NoteDetailIntent.NavigateBack             -> viewModelScope.launch {
                _effect.send(NoteDetailEffect.NavigateBack)
            }
        }
    }

    private fun loadNote(id: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            launch {
                when (val result = getNoteByIdUseCase.fetch(id)) {
                    is Result.Success -> _state.update { current ->
                        current.copy(
                            chatMessages = result.data.messages,
                            editedContent = if (!current.hasUnsavedChanges)
                                result.data.content ?: "" else current.editedContent,
                            isLoading = false
                        )
                    }
                    is Result.Error -> _state.update { it.copy(isLoading = false) }
                    else -> Unit
                }
            }
            getNoteByIdUseCase(id).collect { note ->
                note?.let { n ->
                    _state.update { current ->
                        current.copy(
                            note = n,
                            editedContent = if (!current.hasUnsavedChanges)
                                n.content ?: "" else current.editedContent,
                            isLoading = false
                        )
                    }
                }
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
        val folder = _state.value.folderInput.trim().ifBlank { null }
        _state.update { it.copy(isLoading = true, error = null) }
        when (val result = createNoteUseCase(title, content, useAi, folder)) {
            is Result.Success -> {
                val note = result.data
                // Schedule reminder if enabled
                val s = _state.value
                val hasTime = s.reminderTimeMillis > 0L
                val isRepeating = s.reminderDays.isNotEmpty()
                if (s.reminderEnabled && hasTime && (isRepeating || s.reminderTimeMillis > System.currentTimeMillis())) {
                    _effect.send(
                        NoteDetailEffect.ScheduleReminder(
                            noteId        = note.id,
                            noteTitle     = note.title,
                            reminderTitle = s.reminderTitle.ifBlank { note.title },
                            triggerAtMillis = s.reminderTimeMillis,
                            reminderDays  = s.reminderDays
                        )
                    )
                }
                _effect.send(NoteDetailEffect.NavigateToDetail(note.id))
            }
            is Result.Error -> _state.update { it.copy(error = result.message, isLoading = false) }
            else -> Unit
        }
    }

    private fun saveContent(html: String) = viewModelScope.launch {
        val noteId = _state.value.note?.id ?: return@launch
        _state.update { it.copy(isLoading = true) }
        when (val result = updateNoteUseCase(noteId, content = html)) {
            is Result.Success -> _state.update {
                it.copy(hasUnsavedChanges = false, editedContent = result.data.content ?: "", isLoading = false)
            }
            is Result.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
            else -> Unit
        }
    }

    private fun togglePin() = viewModelScope.launch {
        val noteId = _state.value.note?.id ?: return@launch
        pinNoteUseCase(noteId)
    }

    private fun saveFolder() = viewModelScope.launch {
        val noteId = _state.value.note?.id ?: return@launch
        val folder = _state.value.folderDialogInput.trim().ifBlank { null }
        _state.update { it.copy(showFolderDialog = false) }
        updateNoteUseCase(noteId, folderName = folder ?: "")
    }

    private fun sendChatMessage() = viewModelScope.launch {
        val noteId = _state.value.note?.id ?: return@launch
        val message = _state.value.aiInput.trim()
        if (message.isBlank()) return@launch
        _state.update { it.copy(isAiLoading = true, aiInput = "", chatError = null) }
        when (val result = chatOnNoteUseCase(noteId, message)) {
            is Result.Success -> {
                val aiContent = result.data.messages.lastOrNull { it.role == "assistant" }?.message
                _state.update { current ->
                    current.copy(
                        chatMessages = result.data.messages,
                        editedContent = aiContent ?: current.editedContent,
                        hasUnsavedChanges = aiContent != null,
                        isAiLoading = false,
                        aiContentVersion = if (aiContent != null) current.aiContentVersion + 1 else current.aiContentVersion
                    )
                }
            }
            is Result.Error -> _state.update { it.copy(isAiLoading = false, chatError = result.message) }
            else -> Unit
        }
    }

    private fun sendSelectionEdit() = viewModelScope.launch {
        val noteId = _state.value.note?.id ?: return@launch
        val selectedText = _state.value.selectionText
        val instruction = _state.value.aiInput.trim()
        if (selectedText.isBlank() || instruction.isBlank()) return@launch
        _state.update { it.copy(isSelectionLoading = true) }
        when (val result = refineSelectionUseCase(noteId, selectedText, instruction)) {
            is Result.Success -> _state.update {
                it.copy(isSelectionLoading = false, selectionReplacement = result.data)
            }
            is Result.Error -> _state.update { it.copy(isSelectionLoading = false, chatError = result.message) }
            else -> Unit
        }
    }
}
