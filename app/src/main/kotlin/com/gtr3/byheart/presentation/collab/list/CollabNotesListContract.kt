package com.gtr3.byheart.presentation.collab.list

import com.gtr3.byheart.domain.model.CollabNote

data class CollabNotesListState(
    val isLoading: Boolean = false,
    val notes: List<CollabNote> = emptyList(),
    val error: String? = null,
    // Create flow
    val showCreateSheet: Boolean = false,
    val createTitle: String = "",
    val createUseAi: Boolean = false,
    val isCreating: Boolean = false,
    // Join flow
    val showJoinSheet: Boolean = false,
    val joinCode: String = "",
    val isJoining: Boolean = false,
    val joinError: String? = null
)

sealed class CollabNotesListIntent {
    data object LoadNotes : CollabNotesListIntent()
    // Create sheet
    data object ShowCreateSheet : CollabNotesListIntent()
    data object DismissCreateSheet : CollabNotesListIntent()
    data class CreateTitleChanged(val value: String) : CollabNotesListIntent()
    data class CreateUseAiToggled(val enabled: Boolean) : CollabNotesListIntent()
    data object CreateNote : CollabNotesListIntent()
    // Join sheet
    data object ShowJoinSheet : CollabNotesListIntent()
    data object DismissJoinSheet : CollabNotesListIntent()
    data class JoinCodeChanged(val value: String) : CollabNotesListIntent()
    data object JoinNote : CollabNotesListIntent()
}

sealed class CollabNotesListEffect {
    data class NavigateToDetail(val shareCode: String) : CollabNotesListEffect()
    data class NavigateToCreate(val shareCode: String) : CollabNotesListEffect()
}
