package com.beeftech.backend.api.auth

class AuthService(private val jwtService: JwtService) {

    fun login(
        username: String,
        password: String
    ): LoginResult {

        val currentState =
            loginAttempts[username]

        val now =
            System.currentTimeMillis()

        if (
            currentState?.lockedUntil != null &&
            currentState.lockedUntil > now
        ) {

            val remainingSeconds =
                (currentState.lockedUntil - now) / 1000

            return LoginResult.Locked(
                remainingSeconds
            )
        }

        val validCredentials =
            username == "admin" &&
                    password == "admin123"

        if (validCredentials) {

            loginAttempts.remove(username)

            return LoginResult.Success(
                jwtService.generateToken(username)
            )
        }

        val failedAttempts =
            (currentState?.failedAttempts ?: 0) + 1

        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {

            val lockedUntil =
                now + LOCK_DURATION_MS

            loginAttempts[username] =
                LoginSecurityState(
                    failedAttempts,
                    lockedUntil
                )

            return LoginResult.Locked(
                LOCK_DURATION_MS / 1000
            )
        }

        loginAttempts[username] =
            LoginSecurityState(
                failedAttempts,
                null
            )

        return LoginResult.Failure(
            "Invalid credentials"
        )
    }

    fun register(
        username: String,
        password: String
    ): Boolean {
        // TODO Persist user when database is available
        return true
    }

    private val loginAttempts =
        mutableMapOf<String, LoginSecurityState>()

    private companion object {

        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCK_DURATION_MS = 5 * 60 * 1000L
    }
}