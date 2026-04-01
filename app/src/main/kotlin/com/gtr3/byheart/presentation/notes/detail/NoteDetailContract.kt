package com.gtr3.byheart.presentation.notes.detail

import com.gtr3.byheart.domain.model.Message
import com.gtr3.byheart.domain.model.Note

data class NoteDetailState(
    // Loading/creation state
    val isLoading: Boolean = false,
    val note: Note? = null,
    val error: String? = null,

    // Create-form fields
    val titleInput: String = "",
    val contentInput: String = "",
    val useAi: Boolean = false,
    val folderInput: String = "",

    // View/edit fields
    val editedContent: String = "",
    val hasUnsavedChanges: Boolean = false,
    // Bumped each time AI produces new content so the screen can reload the editor
    val aiContentVersion: Int = 0,

    // Unified AI input (used for both global chat and selection refine)
    val aiInput: String = "",
    val isAiLoading: Boolean = false,
    val chatMessages: List<Message> = emptyList(),
    val chatError: String? = null,

    // Folder dialog
    val showFolderDialog: Boolean = false,
    val folderDialogInput: String = "",

    // Selection AI editing
    val selectionText: String = "",
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
    val isSelectionLoading: Boolean = false,
    val selectionReplacement: String? = null,

    // Reminder (create-form only)
    val reminderEnabled: Boolean = false,
    val reminderTitle: String = "",
    val reminderTimeMillis: Long = 0L,
    // Days of week to repeat: 0=Mon … 6=Sun; empty = one-time reminder
    val reminderDays: Set<Int> = emptySet(),
)

sealed class NoteDetailIntent {
    // Load existing note
    data class LoadNote(val id: Long) : NoteDetailIntent()

    // Create-form intents
    data class TitleChanged(val value: String) : NoteDetailIntent()
    data class ContentChanged(val value: String) : NoteDetailIntent()
    data class UseAiToggled(val enabled: Boolean) : NoteDetailIntent()
    data class FolderInputChanged(val value: String) : NoteDetailIntent()
    data object CreateNote : NoteDetailIntent()

    // View/edit intents
    data class EditedContentChanged(val value: String) : NoteDetailIntent()
    data class SaveContent(val html: String) : NoteDetailIntent()
    data object TogglePin : NoteDetailIntent()
    data object ShowFolderDialog : NoteDetailIntent()
    data object DismissFolderDialog : NoteDetailIntent()
    data class FolderDialogInputChanged(val value: String) : NoteDetailIntent()
    data object SaveFolder : NoteDetailIntent()

    // Unified AI input
    data class AiInputChanged(val value: String) : NoteDetailIntent()
    data object SendAiMessage : NoteDetailIntent()    // routes to chat or selection based on state

    // Selection AI editing
    data class TextSelected(val text: String, val start: Int, val end: Int) : NoteDetailIntent()
    data object ApplySelectionReplacement : NoteDetailIntent()
    data object DiscardSelectionReplacement : NoteDetailIntent()
    data object DismissSelectionEdit : NoteDetailIntent()

    // Reminder intents
    data object ReminderToggled : NoteDetailIntent()
    data class ReminderTitleChanged(val value: String) : NoteDetailIntent()
    data class ReminderTimeChanged(val millis: Long) : NoteDetailIntent()
    data class ReminderDayToggled(val dayIndex: Int) : NoteDetailIntent()   // 0=Mon … 6=Sun

    data object NavigateBack : NoteDetailIntent()
}

sealed class NoteDetailEffect {
    data object NavigateBack : NoteDetailEffect()
    data class NavigateToDetail(val id: Long) : NoteDetailEffect()
    data class ShowError(val message: String) : NoteDetailEffect()
    data class ScheduleReminder(
        val noteId: Long,
        val noteTitle: String,
        val reminderTitle: String,
        val triggerAtMillis: Long,
        val reminderDays: Set<Int>   // empty = one-time
    ) : NoteDetailEffect()
}
