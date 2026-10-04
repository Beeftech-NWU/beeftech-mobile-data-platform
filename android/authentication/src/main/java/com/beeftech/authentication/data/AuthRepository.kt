package com.beeftech.authentication.data

import android.os.Build
import com.beeftech.authentication.domain.LoggedInUser
import com.beeftech.database.dao.UserDao
import com.beeftech.database.dao.PendingSyncDao
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
    data object DeviceRevoked : LoginOutcome()

    /* The server ended this account's access; sign in online to use the app again. */
    data object AccessRevoked : LoginOutcome()
    data object NeedsFirstOnlineLogin : LoginOutcome()
    data class Unavailable(val message: String) : LoginOutcome()
}

class AuthRepository(
    private val apiClient: AuthApiClient,
    private val sessionStore: SessionStore,
    private val userDao: UserDao,
    private val lockoutManager: PinLockoutManager,
    private val deviceIdProvider: DeviceIdProvider,
    private val pendingSyncDao: PendingSyncDao? = null
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
                    deviceId = deviceId,
                    siteId = dto.user.siteId
                )

                /*
                 * BEEFTECH_IDENTITY_RECONCILIATION
                 *
                 * The backend can legitimately return a different
                 * user ID for the same authenticated username when
                 * development server data has been recreated.
                 *
                 * The username + successful PIN authentication tells
                 * us that this is the same account. Therefore local
                 * pending operations owned by the previous cached ID
                 * can safely follow the authenticated identity.
                 */
                val existingUserRow =
                    userDao.getUserByUsername(
                        username
                    )

                val serverUserId =
                    dto.user.userId

                if (
                    existingUserRow != null &&
                    existingUserRow.userId != serverUserId
                ) {

                    pendingSyncDao
                        ?.reassignUserOperations(
                            oldUserId =
                                existingUserRow.userId,
                            newUserId =
                                serverUserId
                        )
                }

                /*
                 * Canonicalise the cached user row to the ID supplied
                 * by the successful online authentication.
                 *
                 * insertUser uses REPLACE, and username is unique,
                 * so an old cached row for the same username is
                 * replaced by this server-backed identity.
                 */
                userDao.insertUser(
                    User(
                        userId =
                            serverUserId,

                        username =
                            dto.user.username,

                        pinHash =
                            dto.user.pinHash,

                        failedPinAttempts =
                            0,

                        role =
                            dto.user.role
                                ?.toLong(),

                        deviceAssignedId =
                            dto.user.deviceAssignedId
                                ?: deviceId,

                        siteId =
                            dto.user.siteId,

                        deviceLastSync =
                            existingUserRow
                                ?.deviceLastSync,

                        failedSyncAttempts =
                            existingUserRow
                                ?.failedSyncAttempts
                                ?: 0
                    )
                )

                /*
                 * Save the authenticated session only after the local
                 * identity and queue ownership agree.
                 */
                sessionStore.save(
                    token =
                        dto.token,

                    expiresAt =
                        expiresAtMillis,

                    user =
                        loggedInUser
                )

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

            is LoginApiResult.DeviceRevoked -> {
                LoginOutcome.DeviceRevoked
            }

            is LoginApiResult.Error -> {
                LoginOutcome.Unavailable(result.message)
            }

            is LoginApiResult.NoNetwork -> {
                val cachedUser = userDao.getUserByUsername(username)
                if (cachedUser != null && sessionStore.revokedUserId() == cachedUser.userId) {
                    /*
                     * The server ended this account's access while we were online. The cached PIN
                     * must not grant local access any more; it is dropped here and the user signs
                     * in online again. Queued records are untouched.
                     */
                    userDao.updatePinHash(cachedUser.userId, null)
                    LoginOutcome.AccessRevoked
                } else if (cachedUser == null || cachedUser.pinHash.isNullOrBlank()) {
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
                            deviceId = deviceId,
                            siteId = cachedUser.siteId
                        )
                        /*
                         * Offline authentication grants LOCAL access
                         * only.
                         *
                         * Do not invent or extend a bearer token for
                         * Render. SessionStore keeps the local
                         * seven-day window separate from the actual
                         * server JWT expiry.
                         */
                        val offlineExpiresAt =
                            System.currentTimeMillis() +
                                7 * 24 * 60 * 60 * 1000L

                        sessionStore.saveOffline(
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
