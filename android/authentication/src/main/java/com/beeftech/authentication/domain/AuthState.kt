package com.beeftech.authentication.domain

sealed class AuthState {
    data object LoggedOut : AuthState()
    data class LoggedIn(val user: LoggedInUser) : AuthState()
    data class Locked(val untilMillis: Long) : AuthState()
}
