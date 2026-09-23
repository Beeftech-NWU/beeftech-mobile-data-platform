package com.beeftech.authentication.data

import android.os.Build
import com.beeftech.authentication.domain.LoggedInUser
import com.beeftech.database.dao.UserDao
import com.beeftech.database.entity.User
import com.beeftech.database.security.PinLockoutManager
import org.mindrot.jbcrypt.BCrypt
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

sealed class LoginOutcome {
    data class Success(val user: LoggedInUser) : LoginOutcome()
    data object BadCredentials : LoginOutcome()
    data class Locked(val untilMillis: Long) : LoginOutcome()
    data object WrongDevice : LoginOutcome()
    data object NeedsFirstOnlineLogin : LoginOutcome()
    data class Unavailable(val message: String) : LoginOutcome()
}

class AuthRepository(
    private val apiClient: AuthApiClient,
    private val sessionStore: SessionStore,
    private val userDao: UserDao,
    private val lockoutManager: PinLockoutManager,
    private val deviceIdProvider: DeviceIdProvider
) {

    suspend fun login(username: String, pin: String): LoginOutcome {

        if (lockoutManager.isLockedOut()) {
            return LoginOutcome.Locked(lockoutManager.getRemainingLockoutTimeMs())
        }

        val deviceId = deviceIdProvider.getDeviceId()
        val result = apiClient.login(
            username = username,
            pin = pin,
            deviceId = deviceId
        )

        return when (result) {
            is LoginApiResult.Success -> {
                val dto = result.response
                val expiresAtMillis = parseIsoToMillis(dto.expiresAt)

                val loggedInUser = LoggedInUser(
                    userId = dto.user.userId,
                    username = dto.user.username,
                    role = dto.user.role,
                    deviceId = deviceId
                )

                sessionStore.save(
                    token = dto.token,
                    expiresAt = expiresAtMillis,
                    user = loggedInUser
                )

                val existingUserRow = userDao.getUserByUsername(username)
                if (existingUserRow != null) {
                    userDao.updateUser(
                        existingUserRow.copy(
                            pinHash = dto.user.pinHash,
                            failedPinAttempts = 0,
                            role = dto.user.role?.toLong(),
                            deviceAssignedId = dto.user.deviceAssignedId ?: deviceId
                        )
                    )
                } else {
                    userDao.insertUser(
                        User(
                            userId = dto.user.userId,
                            username = dto.user.username,
                            pinHash = dto.user.pinHash,
                            failedPinAttempts = 0,
                            role = dto.user.role?.toLong(),
                            deviceAssignedId = dto.user.deviceAssignedId ?: deviceId
                        )
                    )
                }

                lockoutManager.resetAttempts()
                LoginOutcome.Success(loggedInUser)
            }

            is LoginApiResult.Unauthorized -> {
                lockoutManager.recordFailedAttempt()
                if (lockoutManager.isLockedOut()) {
                    LoginOutcome.Locked(lockoutManager.getRemainingLockoutTimeMs())
                } else {
                    LoginOutcome.BadCredentials
                }
            }

            is LoginApiResult.Locked -> {
                LoginOutcome.Locked(result.remainingSeconds * 1000L)
            }

            is LoginApiResult.WrongDevice -> {
                LoginOutcome.WrongDevice
            }

            is LoginApiResult.Error -> {
                LoginOutcome.Unavailable(result.message)
            }

            is LoginApiResult.NoNetwork -> {
                val cachedUser = userDao.getUserByUsername(username)
                if (cachedUser == null || cachedUser.pinHash.isNullOrBlank()) {
                    LoginOutcome.NeedsFirstOnlineLogin
                } else {
                    val passwordMatches = try {
                        BCrypt.checkpw(pin, cachedUser.pinHash)
                    } catch (e: Exception) {
                        false
                    }

                    if (passwordMatches) {
                        val loggedInUser = LoggedInUser(
                            userId = cachedUser.userId,
                            username = cachedUser.username,
                            role = cachedUser.role?.toInt(),
                            deviceId = deviceId
                        )
                        val existingToken = sessionStore.token() ?: "OFFLINE_TOKEN_${System.currentTimeMillis()}"
                        val offlineExpiresAt = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L

                        sessionStore.save(
                            token = existingToken,
                            expiresAt = offlineExpiresAt,
                            user = loggedInUser
                        )

                        lockoutManager.resetAttempts()
                        LoginOutcome.Success(loggedInUser)
                    } else {
                        lockoutManager.recordFailedAttempt()
                        if (lockoutManager.isLockedOut()) {
                            LoginOutcome.Locked(lockoutManager.getRemainingLockoutTimeMs())
                        } else {
                            LoginOutcome.BadCredentials
                        }
                    }
                }
            }
        }
    }

    fun logout() {
        sessionStore.clear()
    }

    fun currentUser(): LoggedInUser? {
        return sessionStore.currentUser()
    }

    private fun parseIsoToMillis(isoString: String): Long {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Instant.parse(isoString).toEpochMilli()
            } else {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                sdf.parse(isoString)?.time ?: (System.currentTimeMillis() + 24 * 60 * 60 * 1000L)
            }
        } catch (e: Exception) {
            System.currentTimeMillis() + 24 * 60 * 60 * 1000L
        }
    }
}
