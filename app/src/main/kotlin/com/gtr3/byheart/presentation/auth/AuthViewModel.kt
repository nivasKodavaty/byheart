package com.gtr3.byheart.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtr3.byheart.core.util.Result
import com.gtr3.byheart.domain.repository.AuthRepository
import com.gtr3.byheart.domain.usecase.auth.LoginUseCase
import com.gtr3.byheart.domain.usecase.auth.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    private val _effect = Channel<AuthEffect>()
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.EmailChanged    -> _state.update { it.copy(email = intent.value) }
            is AuthIntent.PasswordChanged -> _state.update { it.copy(password = intent.value) }
            AuthIntent.Login              -> login()
            AuthIntent.Register           -> register()
            is AuthIntent.GoogleSignIn    -> loginWithGoogle(intent.idToken)
        }
    }

    private fun login() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        when (val result = loginUseCase(_state.value.email, _state.value.password)) {
            is Result.Success -> {
                authRepository.saveToken(result.data.token, result.data.refreshToken)
                _effect.send(AuthEffect.NavigateToNotes)
            }
            is Result.Error   -> _state.update { it.copy(error = result.message) }
            else              -> Unit
        }
        _state.update { it.copy(isLoading = false) }
    }

    private fun loginWithGoogle(idToken: String) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        when (val result = authRepository.loginWithGoogle(idToken)) {
            is Result.Success -> {
                authRepository.saveToken(result.data.token, result.data.refreshToken)
                _effect.send(AuthEffect.NavigateToNotes)
            }
            is Result.Error   -> _state.update { it.copy(error = result.message) }
            else              -> Unit
        }
        _state.update { it.copy(isLoading = false) }
    }

    private fun register() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        when (val result = registerUseCase(_state.value.email, _state.value.password)) {
            is Result.Success -> {
                authRepository.saveToken(result.data.token, result.data.refreshToken)
                _effect.send(AuthEffect.NavigateToNotes)
            }
            is Result.Error   -> _state.update { it.copy(error = result.message) }
            else              -> Unit
        }
        _state.update { it.copy(isLoading = false) }
    }
}
