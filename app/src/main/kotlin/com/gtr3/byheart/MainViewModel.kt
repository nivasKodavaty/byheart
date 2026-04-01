package com.gtr3.byheart

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtr3.byheart.domain.repository.AuthRepository
import com.gtr3.byheart.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // null = still checking; non-null = ready to navigate
    var startDestination by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            startDestination = if (authRepository.getToken() != null) {
                Screen.Main.route
            } else {
                Screen.Login.route
            }
        }
    }
}
