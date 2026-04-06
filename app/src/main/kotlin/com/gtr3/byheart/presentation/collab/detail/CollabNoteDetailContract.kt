package com.gtr3.byheart.presentation.collab.detail

import com.gtr3.byheart.domain.model.CollabNote
import com.gtr3.byheart.domain.model.CollabParticipant
import com.gtr3.byheart.domain.model.Message

data class CollabNoteDetailState(
    val isLoading: Boolean = false,
    val note: CollabNote? = null,
    val error: String? = null,

    // Editing
    val editedTitle: String = "",
    val editedContent: String = "",
    val hasUnsavedChanges: Boolean = false,
    val isSaving: Boolean = false,

    // AI content version bump triggers editor reload
    val aiContentVersion: Int = 0,

    // AI chat / unified input
    val aiInput: String = "",
    val isAiLoading: Boolean = false,
    val chatMessages: List<Message> = emptyList(),
    val chatError: String? = null,

    // Selection AI editing
    val selectionText: String = "",
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
    val isSelectionLoading: Boolean = false,
    val selectionReplacement: String? = null,

    // Conflict handling — set when a 409 is received
    val conflictContent: String? = null,
    val conflictVersion: Long? = null,

    // Reminder
    val reminderEnabled: Boolean = false,
    val reminderTitle: String = "",
    val reminderTimeMillis: Long = 0L,
    val reminderDays: Set<Int> = emptySet(),

    // Share code display
    val showShareCodeSheet: Boolean = false,

    // Participants sheet
    val participants: List<CollabParticipant> = emptyList(),
    val showParticipantsSheet: Boolean = false,
    val isParticipantsLoading: Boolean = false,

    // Reminder sheet (post-creation)
    val showReminderSheet: Boolean = false
)

sealed class CollabNoteDetailIntent {
    data class Load(val shareCode: String) : CollabNoteDetailIntent()
    data class TitleChanged(val value: String) : CollabNoteDetailIntent()
    data class EditedContentChanged(val html: String) : CollabNoteDetailIntent()
    data class SaveContent(val html: String) : CollabNoteDetailIntent()
    data object PollForUpdates : CollabNoteDetailIntent()
    data object AcceptConflict : CollabNoteDetailIntent()    // user accepts latest from server
    data object ShowShareCode : CollabNoteDetailIntent()
    data object DismissShareCode : CollabNoteDetailIntent()
    data object LeaveNote : CollabNoteDetailIntent()
    // AI
    data class AiInputChanged(val value: String) : CollabNoteDetailIntent()
    data object SendAiMessage : CollabNoteDetailIntent()
    data class TextSelected(val text: String, val start: Int, val end: Int) : CollabNoteDetailIntent()
    data object ApplySelectionReplacement : CollabNoteDetailIntent()
    data object DiscardSelectionReplacement : CollabNoteDetailIntent()
    data object DismissSelectionEdit : CollabNoteDetailIntent()
    // Reminder
    data object ReminderToggled : CollabNoteDetailIntent()
    data class ReminderTitleChanged(val value: String) : CollabNoteDetailIntent()
    data class ReminderTimeChanged(val millis: Long) : CollabNoteDetailIntent()
    data class ReminderDayToggled(val dayIndex: Int) : CollabNoteDetailIntent()
    data object NavigateBack : CollabNoteDetailIntent()
    // Participants
    data object ShowParticipants : CollabNoteDetailIntent()
    data object DismissParticipants : CollabNoteDetailIntent()
    // Reminder sheet
    data object OpenReminderSheet : CollabNoteDetailIntent()
    data object DismissReminderSheet : CollabNoteDetailIntent()
    data object ScheduleReminderNow : CollabNoteDetailIntent()
}

sealed class CollabNoteDetailEffect {
    data object NavigateBack : CollabNoteDetailEffect()
    data object NoteLeft : CollabNoteDetailEffect()
    /** Informs screen to reload the rich-text editor with new AI content */
    data object ReloadEditor : CollabNoteDetailEffect()
    /** 409 received — screen should show conflict snackbar and reload editor */
    data class ConflictDetected(val latestContent: String?, val latestVersion: Long) : CollabNoteDetailEffect()
    data class ShowError(val message: String) : CollabNoteDetailEffect()
    data class ScheduleReminder(
        val noteId: Long,
        val noteTitle: String,
        val reminderTitle: String,
        val triggerAtMillis: Long,
        val reminderDays: Set<Int>
    ) : CollabNoteDetailEffect()
}
