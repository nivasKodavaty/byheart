package com.gtr3.byheart.presentation.auth

data class AuthState(
    val isLoading: Boolean = false,
    val email: String = "",
    val password: String = "",
    val error: String? = null
)

sealed class AuthIntent {
    data class EmailChanged(val value: String) : AuthIntent()
    data class PasswordChanged(val value: String) : AuthIntent()
    data object Login : AuthIntent()
    data object Register : AuthIntent()
}

sealed class AuthEffect {
    data object NavigateToNotes : AuthEffect()
    data class ShowError(val message: String) : AuthEffect()
}
