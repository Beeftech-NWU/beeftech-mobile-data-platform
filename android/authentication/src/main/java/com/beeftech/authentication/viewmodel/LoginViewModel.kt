package com.beeftech.authentication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beeftech.authentication.data.AuthRepository
import com.beeftech.authentication.data.LoginOutcome
import com.beeftech.authentication.domain.LoggedInUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Busy : LoginUiState()
    data class Error(val message: String) : LoginUiState()
    data class Done(val user: LoggedInUser) : LoginUiState()
}

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(username: String, pin: String) {
        if (username.isBlank() || pin.isBlank()) {
            _uiState.value = LoginUiState.Error("Username and PIN are required")
            return
        }

        _uiState.value = LoginUiState.Busy

        viewModelScope.launch {
            val outcome = authRepository.login(username.trim(), pin.trim())
            _uiState.value = when (outcome) {
                is LoginOutcome.Success -> LoginUiState.Done(outcome.user)
                is LoginOutcome.BadCredentials -> LoginUiState.Error("Incorrect username or PIN")
                is LoginOutcome.Locked -> {
                    val minutes = (outcome.untilMillis / 1000 / 60).coerceAtLeast(1)
                    LoginUiState.Error("Too many attempts. Try again in $minutes minutes.")
                }
                is LoginOutcome.WrongDevice -> LoginUiState.Error("This phone is registered to another worker")
                is LoginOutcome.NeedsFirstOnlineLogin -> LoginUiState.Error("Connect once to set up this account")
                is LoginOutcome.Unavailable -> LoginUiState.Error(
                    outcome.message.ifBlank { "Cannot reach the server. Try again." }
                )
            }
        }
    }

    fun clearError() {
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }
}

class LoginViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
