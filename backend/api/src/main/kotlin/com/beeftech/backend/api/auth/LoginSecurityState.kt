package com.beeftech.backend.api.auth

data class LoginSecurityState(
    val failedAttempts: Int,
    val lockedUntil: Long?
)