package com.beeftech.backend.api.auth

sealed class LoginResult {

    data class Success(
        val token: String
    ) : LoginResult()

    data class Failure(
        val message: String
    ) : LoginResult()

    data class Locked(
        val remainingSeconds: Long
    ) : LoginResult()
}