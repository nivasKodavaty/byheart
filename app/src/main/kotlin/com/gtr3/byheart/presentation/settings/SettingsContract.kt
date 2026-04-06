package com.gtr3.byheart.presentation.settings

data class SettingsState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val email: String = "",
    val displayName: String = "",
    val dateOfBirth: String = "",   // "YYYY-MM-DD" or ""
    val sex: String = "",
    val error: String? = null
)

sealed class SettingsIntent {
    data class DisplayNameChanged(val value: String) : SettingsIntent()
    data class DateOfBirthChanged(val value: String) : SettingsIntent()
    data class SexChanged(val value: String) : SettingsIntent()
    data object Save : SettingsIntent()
    data object Logout : SettingsIntent()
    data object DismissError : SettingsIntent()
}

sealed class SettingsEffect {
    data object NavigateToLogin : SettingsEffect()
    data object SaveSuccess : SettingsEffect()
}
