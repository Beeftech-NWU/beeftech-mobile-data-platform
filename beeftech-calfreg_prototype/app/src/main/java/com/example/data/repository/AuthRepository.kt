package com.example.data.repository

import com.example.data.model.UserRole
import com.example.data.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

class AuthRepository {
    private val _currentSession = MutableStateFlow(
        UserSession.DEFAULT_USERS.first { it.role == UserRole.FIELD_WORKER }
    )
    val currentSession: StateFlow<UserSession> = _currentSession.asStateFlow()

    // Map username to salted PIN hash (salt: "beeftech_salt_2026_")
    private val pinHashes = mapOf(
        "field_worker" to hashPin("1234"),
        "office_admin" to hashPin("2345"),
        "sys_admin" to hashPin("9999")
    )

    fun login(username: String, pin: String): Boolean {
        val expectedHash = pinHashes[username] ?: return false
        val providedHash = hashPin(pin)
        if (expectedHash == providedHash) {
            val user = UserSession.DEFAULT_USERS.find { it.username == username } ?: return false
            _currentSession.value = user.copy(isLoggedIn = true)
            return true
        }
        return false
    }

    fun switchRole(role: UserRole) {
        val user = UserSession.DEFAULT_USERS.find { it.role == role } ?: return
        _currentSession.value = user.copy(isLoggedIn = true)
    }

    fun logout() {
        val current = _currentSession.value
        _currentSession.value = current.copy(isLoggedIn = false)
    }

    companion object {
        fun hashPin(pin: String): String {
            val salted = "beeftech_salt_2026_$pin"
            val bytes = MessageDigest.getInstance("SHA-256").digest(salted.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
