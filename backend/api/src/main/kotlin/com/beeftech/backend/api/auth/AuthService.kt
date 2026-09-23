package com.beeftech.backend.api.auth

import java.time.Instant
import java.time.format.DateTimeFormatter

class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService
) {

    suspend fun login(
        username: String,
        pin: String,
        deviceId: String
    ): LoginResult {

        val currentState = loginAttempts[username]
        val now = System.currentTimeMillis()

        if (currentState?.lockedUntil != null && currentState.lockedUntil > now) {
            val remainingSeconds = (currentState.lockedUntil - now) / 1000
            return LoginResult.Locked(remainingSeconds)
        }

        val user = userRepository.findByUsername(username)

        if (user == null) {
            return recordFailedAttempt(username, now)
        }

        if (!PinHasher.verify(pin, user.pinHash)) {
            return recordFailedAttempt(username, now)
        }

        if (user.deviceAssignedId != null && user.deviceAssignedId != deviceId) {
            return LoginResult.WrongDevice
        }

        if (user.deviceAssignedId == null) {
            userRepository.claimDevice(user.userId, deviceId)
        }

        userRepository.touchLastSync(user.userId)
        loginAttempts.remove(username)

        val token = jwtService.generateToken(
            username = user.username,
            userId = user.userId,
            role = user.role,
            deviceId = deviceId
        )

        val expiresAt = DateTimeFormatter.ISO_INSTANT.format(
            Instant.ofEpochMilli(now + 24 * 60 * 60 * 1000L)
        )

        val profile = UserProfile(
            userId = user.userId,
            username = user.username,
            role = user.role,
            pinHash = user.pinHash,
            deviceAssignedId = user.deviceAssignedId ?: deviceId
        )

        return LoginResult.Success(
            token = token,
            expiresAt = expiresAt,
            profile = profile
        )
    }

    fun register(
        username: String,
        password: String
    ): Boolean {
        return true
    }

    private fun recordFailedAttempt(
        username: String,
        now: Long
    ): LoginResult {

        val currentState = loginAttempts[username]
        val failedAttempts = (currentState?.failedAttempts ?: 0) + 1

        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {

            val lockedUntil = now + LOCK_DURATION_MS

            loginAttempts[username] = LoginSecurityState(
                failedAttempts,
                lockedUntil
            )

            return LoginResult.Locked(LOCK_DURATION_MS / 1000)
        }

        loginAttempts[username] = LoginSecurityState(
            failedAttempts,
            null
        )

        return LoginResult.Failure("Incorrect username or PIN")
    }

    private val loginAttempts = mutableMapOf<String, LoginSecurityState>()

    private companion object {

        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCK_DURATION_MS = 5 * 60 * 1000L
    }
}
