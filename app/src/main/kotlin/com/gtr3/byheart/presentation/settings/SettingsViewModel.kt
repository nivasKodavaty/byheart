package com.gtr3.byheart.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtr3.byheart.core.util.Result
import com.gtr3.byheart.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    private val _effect = Channel<SettingsEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch { loadProfile() }
    }

    private suspend fun loadProfile() {
        when (val result = authRepository.getProfile()) {
            is Result.Success -> _state.update {
                it.copy(
                    isLoading   = false,
                    email       = result.data.email,
                    displayName = result.data.displayName ?: "",
                    dateOfBirth = result.data.dateOfBirth ?: "",
                    sex         = result.data.sex ?: ""
                )
            }
            is Result.Error -> _state.update {
                it.copy(isLoading = false, error = result.message)
            }
            else -> Unit
        }
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.DisplayNameChanged -> _state.update { it.copy(displayName = intent.value) }
            is SettingsIntent.DateOfBirthChanged -> _state.update { it.copy(dateOfBirth = intent.value) }
            is SettingsIntent.SexChanged         -> _state.update { it.copy(sex = intent.value) }
            SettingsIntent.Save                  -> saveProfile()
            SettingsIntent.Logout                -> logout()
            SettingsIntent.DismissError          -> _state.update { it.copy(error = null) }
        }
    }

    private fun saveProfile() = viewModelScope.launch {
        val s = _state.value
        _state.update { it.copy(isSaving = true) }
        when (val result = authRepository.updateProfile(
            displayName = s.displayName.ifBlank { null },
            dateOfBirth = s.dateOfBirth.ifBlank { null },
            sex         = s.sex.ifBlank { null }
        )) {
            is Result.Success -> {
                _state.update { it.copy(isSaving = false) }
                _effect.send(SettingsEffect.SaveSuccess)
            }
            is Result.Error -> _state.update { it.copy(isSaving = false, error = result.message) }
            else -> _state.update { it.copy(isSaving = false) }
        }
    }

    private fun logout() = viewModelScope.launch {
        authRepository.clearToken()
        _effect.send(SettingsEffect.NavigateToLogin)
    }
}
