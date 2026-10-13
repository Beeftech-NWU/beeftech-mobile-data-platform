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

/**
 * A fresh PIN-authenticated manager session may reconcile a changed backend-generated user ID
 * only when the same username, site, role and bound device are verified. It never changes
 * a farmer's site or role and never mixes pending work from different accounts.
 */
sealed class OnlineSessionRenewal {
    data object Renewed : OnlineSessionRenewal()
    data class RequiresFullLogin(val message: String) : OnlineSessionRenewal()
    data class Rejected(val message: String) : OnlineSessionRenewal()
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
                    siteId = dto.user.siteId,
                    farmCode = dto.user.farmCode
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

    /**
     * Renew an authenticated manager session without logging out or discarding offline work.
     * A re-created backend may generate a different user ID for the SAME account. Only
     * that ID difference can be reconciled automatically; site/role changes are blocked.
     */
    suspend fun renewOnlineSession(pin: String): OnlineSessionRenewal {
        val existing = sessionStore.currentUser()
            ?: return OnlineSessionRenewal.RequiresFullLogin("Sign in to BeefTech first.")
        if (sessionStore.isExpired()) {
            return OnlineSessionRenewal.RequiresFullLogin("Your local session ended. Sign in online again.")
        }
        if (pin.isBlank()) return OnlineSessionRenewal.Rejected("Enter your PIN.")
        if (lockoutManager.isLockedOut()) {
            return OnlineSessionRenewal.Rejected("Too many attempts. Try again later.")
        }
        val deviceId = deviceIdProvider.getDeviceId()
        if (existing.deviceId != deviceId) {
            return OnlineSessionRenewal.RequiresFullLogin(
                "The device identity changed. Sign in online to verify this device."
            )
        }
        val reply = apiClient.login(existing.username, pin, deviceId)
        return when (reply) {
            is LoginApiResult.Success -> {
                val online = reply.response.user
                // A valid PIN never authorizes shifting another user's offline data to this user.
                when {
                    !online.username.equals(existing.username, ignoreCase = true) ->
                        OnlineSessionRenewal.RequiresFullLogin(
                            "The authenticated username differs from the offline account. Sign in online."
                        )
                    online.role != existing.role ->
                        OnlineSessionRenewal.RequiresFullLogin(
                            "The server role differs from the offline account. Ask the administrator to verify access."
                        )
                    // Site-free managers are not safe to reconcile. Admin continues to use
                    // normal login when a backend-generated user ID changes.
                    online.siteId != existing.siteId ->
                        OnlineSessionRenewal.RequiresFullLogin(
                            "The assigned farm site differs from the offline account. Ask the administrator to verify the site before synchronizing."
                        )
                    online.deviceAssignedId != null && online.deviceAssignedId != deviceId ->
                        OnlineSessionRenewal.RequiresFullLogin(
                            "The server associates this account with a different device. Sign in online."
                        )
                    else -> {
                        val expiry = parseIsoToMillis(reply.response.expiresAt)
                        if (expiry <= System.currentTimeMillis()) {
                            OnlineSessionRenewal.Rejected(
                                "The server issued an expired session. Try signing in online again."
                            )
                        } else {
                            val newUser = existing.copy(
                                userId = online.userId,
                                farmCode = online.farmCode ?: existing.farmCode
                            )
                            if (online.userId != existing.userId) {
                                // This is a *same-site/role* reconciliation, never a cross-site transfer.
                                val cached = userDao.getUserByUsername(existing.username)
                                val other = userDao.getUserById(online.userId)
                                if (existing.siteId.isNullOrBlank() ||
                                    cached == null || cached.userId != existing.userId ||
                                    cached.siteId != existing.siteId ||
                                    cached.role?.toInt() != existing.role ||
                                    (other != null && !other.username.equals(existing.username, ignoreCase = true)) ||
                                    pendingSyncDao == null
                                ) {
                                    return OnlineSessionRenewal.RequiresFullLogin(
                                        "Local account identity cannot be safely reconciled. Sign in online as the same manager."
                                    )
                                }
                                // The normal online-login path already uses this verified-ID
                                // queue migration. Reuse its semantics, then refresh the UI.
                                pendingSyncDao.reassignUserOperations(existing.userId, online.userId)
                                userDao.insertUser(
                                    User(
                                        userId = online.userId,
                                        username = online.username,
                                        pinHash = online.pinHash,
                                        failedPinAttempts = 0,
                                        role = online.role?.toLong(),
                                        deviceAssignedId = online.deviceAssignedId ?: deviceId,
                                        siteId = online.siteId,
                                        deviceLastSync = cached.deviceLastSync,
                                        failedSyncAttempts = cached.failedSyncAttempts
                                    )
                                )
                            }
                            sessionStore.save(reply.response.token, expiry, newUser)
                            if (online.userId != existing.userId) {
                                // AuthGate must not keep showing the old user ID after the
                                // pending queue has moved to the verified server user ID.
                                sessionStore.announceIdentityReconciled()
                            }
                            lockoutManager.resetAttempts()
                            OnlineSessionRenewal.Renewed
                        }
                    }
                }
            }
            is LoginApiResult.Unauthorized -> {
                lockoutManager.recordFailedAttempt()
                OnlineSessionRenewal.Rejected("Incorrect PIN. Check your F Manager credentials.")
            }
            is LoginApiResult.Locked -> OnlineSessionRenewal.Rejected("Account temporarily locked. Try again later.")
            is LoginApiResult.WrongDevice -> OnlineSessionRenewal.Rejected("This device belongs to another account.")
            is LoginApiResult.DeviceRevoked -> OnlineSessionRenewal.Rejected("Device access revoked. Contact your administrator.")
            is LoginApiResult.NoNetwork -> OnlineSessionRenewal.Rejected("Cannot reach Render. Connect to the internet and retry.")
            is LoginApiResult.Error -> OnlineSessionRenewal.Rejected(
                reply.message.ifBlank { "Online authentication failed." }
            )
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
