package com.beeftech.backend.api.auth

import java.time.Instant
import java.time.format.DateTimeFormatter

class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val loginSecurity: LoginSecurityRepository = LoginSecurityRepository(),
    private val deviceRepository: DeviceRepository = DeviceRepository(),
    private val now: () -> Long = System::currentTimeMillis
) {

    suspend fun login(
        username: String,
        pin: String,
        deviceId: String,
        deviceModel: String? = null,
        appVersion: String? = null
    ): LoginResult {

        val time = now()

        /* Looked up first so every outcome, even a bad one, can name the user and site. */
        val user = userRepository.findByUsername(username)

        suspend fun event(outcome: String) =
            loginSecurity.recordEvent(username, user?.userId, deviceId, outcome, user?.siteId, appVersion, time)

        val currentState = loginSecurity.state(username)

        if (currentState?.lockedUntil != null && currentState.lockedUntil > time) {
            event(LoginOutcome.LOCKED)
            return LoginResult.Locked((currentState.lockedUntil - time) / 1000)
        }

        if (user == null) {
            event(LoginOutcome.UNKNOWN_USER)
            return recordFailedAttempt(username, time)
        }

        if (!PinHasher.verify(pin, user.pinHash)) {
            event(LoginOutcome.BAD_CREDENTIALS)
            return recordFailedAttempt(username, time)
        }

        /* After the PIN check, so a wrong PIN can't be used to probe which accounts are deactivated. */
        if (!user.active) {
            event(LoginOutcome.INACTIVE)
            return LoginResult.Failure("This account has been deactivated")
        }

        if (deviceRepository.isRevoked(deviceId)) {
            event(LoginOutcome.DEVICE_REVOKED)
            return LoginResult.DeviceRevoked
        }

        if (user.deviceAssignedId != null && user.deviceAssignedId != deviceId) {
            event(LoginOutcome.WRONG_DEVICE)
            return LoginResult.WrongDevice
        }

        if (user.deviceAssignedId == null) {
            userRepository.claimDevice(user.userId, deviceId)
        }

        userRepository.touchLastSync(user.userId)
        loginSecurity.clear(username)
        deviceRepository.recordLogin(deviceId, deviceModel, appVersion, user.userId, user.siteId, time)
        event(LoginOutcome.SUCCESS)

        val token = jwtService.generateToken(
            username = user.username,
            userId = user.userId,
            role = user.role,
            deviceId = deviceId,
            siteId = user.siteId
        )

        val expiresAt = DateTimeFormatter.ISO_INSTANT.format(
            Instant.ofEpochMilli(time + 24 * 60 * 60 * 1000L)
        )

        val profile = UserProfile(
            userId = user.userId,
            username = user.username,
            role = user.role,
            pinHash = user.pinHash,
            deviceAssignedId = user.deviceAssignedId ?: deviceId,
            siteId = user.siteId
        )

        return LoginResult.Success(
            token = token,
            expiresAt = expiresAt,
            profile = profile
        )
    }

    private suspend fun recordFailedAttempt(
        username: String,
        time: Long
    ): LoginResult {

        val state = loginSecurity.recordFailure(username, time, MAX_FAILED_ATTEMPTS, LOCK_DURATION_MS)

        if (state.lockedUntil != null) {
            return LoginResult.Locked(LOCK_DURATION_MS / 1000)
        }

        return LoginResult.Failure("Incorrect username or PIN")
    }

    private companion object {

        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCK_DURATION_MS = 5 * 60 * 1000L
    }
}
