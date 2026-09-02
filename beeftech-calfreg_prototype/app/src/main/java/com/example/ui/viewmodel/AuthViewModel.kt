package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    val currentSession: StateFlow<UserSession> = authRepository.currentSession

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    fun login(username: String, pin: String): Boolean {
        _loginError.value = null
        val success = authRepository.login(username, pin)
        if (!success) {
            _loginError.value = "Invalid username or 4-digit PIN."
        }
        return success
    }

    fun switchRole(role: UserRole) {
        authRepository.switchRole(role)
    }

    fun logout() {
        authRepository.logout()
    }

    fun clearError() {
        _loginError.value = null
    }
}
