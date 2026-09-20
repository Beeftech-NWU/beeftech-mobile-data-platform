package com.beeftech.authentication.data

import com.beeftech.database.dao.UserDao
import com.beeftech.database.entity.User
import com.beeftech.database.security.PinLockoutManager
import org.mindrot.jbcrypt.BCrypt

/**
 * The only class that knows about online vs offline login, per the login
 * implementation plan. Coordinates [AuthApiClient] (network), [SessionStore]
 * (persisted session/token), [UserDao] (cached USERS row for offline PIN
 * checks), and [PinLockoutManager] (device-wide lockout, reused rather than
 * rebuilt - this is a documented decision, see PinLockoutManager itself for
 * the "device-wide vs per-user" open question the plan raises).
 */
class AuthRepository(
    private val apiClient: AuthApiClient,
    private val sessionStore: SessionStore,
    private val userDao: UserDao,
    private val pinLockoutManager: PinLockoutManager
) {

    suspend fun login(username: String, pin: String): LoginResult {
        if (pinLockoutManager.isLockedOut()) {
            return LoginResult.Locked(pinLockoutManager.getRemainingLockoutTimeMs())
        }

        val deviceId = android.os.Build.MODEL ?: "unknown-device"

        return when (val outcome = apiClient.login(username, pin, deviceId)) {
            is LoginOutcome.Success -> {
                pinLockoutManager.resetAttempts()

                val profile = outcome.response.user

                sessionStore.saveSession(
                    token = outcome.response.token,
                    expiresAt = outcome.response.expiresAt,
                    user = profile
                )

                userDao.insertUser(
                    User(
                        userId = profile.userId,
                        username = profile.username,
                        pinHash = profile.pinHash,
                        role = profile.role,
                        deviceAssignedId = profile.deviceAssignedId
                    )
                )

                LoginResult.Success(
                    user = SessionUser(
                        userId = profile.userId,
                        username = profile.username,
                        role = profile.role,
                        deviceId = profile.deviceAssignedId
                    ),
                    offline = false
                )
            }

            is LoginOutcome.BadCredentials -> {
                pinLockoutManager.recordFailedAttempt()
                LoginResult.BadCredentials
            }

            is LoginOutcome.Locked ->
                LoginResult.Locked(pinLockoutManager.getRemainingLockoutTimeMs())

            is LoginOutcome.WrongDevice -> LoginResult.WrongDevice

            is LoginOutcome.NetworkError -> loginOffline(username, pin)

            is LoginOutcome.UnknownError -> LoginResult.UnknownError(outcome.message)
        }
    }

    /**
     * Offline path: verifies [pin] against the cached [User.pinHash] with
     * BCrypt. Only reachable when [login] hit a genuine connectivity
     * failure - a rejected login (bad credentials/locked/wrong device)
     * never falls back to this.
     */
    private suspend fun loginOffline(username: String, pin: String): LoginResult {
        val cachedUser = userDao.getUserByUsername(username)
            ?: return LoginResult.NeedsOnlineSetup

        val pinHash = cachedUser.pinHash
            ?: return LoginResult.NeedsOnlineSetup

        val matches = try {
            BCrypt.checkpw(pin, pinHash)
        } catch (exception: Exception) {
            false
        }

        if (!matches) {
            pinLockoutManager.recordFailedAttempt()
            return LoginResult.BadCredentials
        }

        pinLockoutManager.resetAttempts()
        sessionStore.saveOfflineSession(cachedUser)

        return LoginResult.Success(
            user = SessionUser(
                userId = cachedUser.userId,
                username = cachedUser.username,
                role = cachedUser.role,
                deviceId = cachedUser.deviceAssignedId
            ),
            offline = true
        )
    }

    fun logout() {
        sessionStore.clear()
    }

     fun currentUser(): SessionUser? {
        val userId = sessionStore.getUserId() ?: return null
        return SessionUser(
            userId = userId,
            username = sessionStore.getUsername() ?: return null,
            role = sessionStore.getRole(),
            deviceId = sessionStore.getDeviceId()
        )
    }
}

data class SessionUser(
    val userId: String,
    val username: String,
    val role: Long?,
    val deviceId: String?
)

sealed class LoginResult {
    data class Success(val user: SessionUser, val offline: Boolean) : LoginResult()
    data object BadCredentials : LoginResult()
    data class Locked(val remainingMs: Long) : LoginResult()
    data object WrongDevice : LoginResult()
    data object NeedsOnlineSetup : LoginResult()
    data class UnknownError(val message: String) : LoginResult()
}

