package com.beeftech.backend.api.auth

import com.beeftech.backend.api.DatabaseFactory
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

sealed interface AuthCheck {
    /* The caller's role and site as the database has them now, not as the token says. */
    data class Ok(val role: Int?, val siteId: String?) : AuthCheck

    data class Rejected(val message: String) : AuthCheck
}

/**
 * Checks a decoded token against the database on every authenticated request, so a
 * deactivated user, a revoked phone or a reset PIN stops working at once instead of
 * when the 24 h token expires.
 */
class AuthStateRepository(
    private val now: () -> Long = System::currentTimeMillis
) {

    suspend fun check(principal: AuthPrincipal): AuthCheck =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            /* A token issued before user_id existed carries the username in its place. */
            val user = UsersTable.selectAll()
                .where { UsersTable.userId eq principal.userId }
                .singleOrNull()
                ?: UsersTable.selectAll()
                    .where { UsersTable.username eq principal.username }
                    .singleOrNull()
                ?: return@newSuspendedTransaction AuthCheck.Rejected(SESSION_REVOKED)

            if (!user[UsersTable.active]) return@newSuspendedTransaction AuthCheck.Rejected(SESSION_REVOKED)

            /* A token without iat_ms counts as issued at time 0: fine until a cut-off is set. */
            val validAfter = user[UsersTable.tokensValidAfter]
            if (validAfter != null && (principal.issuedAtMs ?: 0L) < validAfter) {
                return@newSuspendedTransaction AuthCheck.Rejected(SESSION_REVOKED)
            }

            val time = now()
            val deviceId = principal.deviceId
            if (deviceId != null) {
                val device = DevicesTable.selectAll().where { DevicesTable.deviceId eq deviceId }.singleOrNull()
                if (device != null) {
                    if (device[DevicesTable.status] == DeviceStatus.REVOKED) {
                        return@newSuspendedTransaction AuthCheck.Rejected(DEVICE_REVOKED)
                    }
                    /* Throttled, so a batch of syncs doesn't write on every request. */
                    if (time - device[DevicesTable.lastSeenAt] >= TOUCH_INTERVAL_MS) {
                        DevicesTable.update({ DevicesTable.deviceId eq deviceId }) {
                            it[lastSeenAt] = time
                            it[lastUserId] = user[UsersTable.userId]
                        }
                    }
                }
            }

            /* "Last contact": any authenticated request counts, not only a login. */
            if (time - (user[UsersTable.deviceLastSync] ?: 0L) >= TOUCH_INTERVAL_MS) {
                UsersTable.update({ UsersTable.userId eq user[UsersTable.userId] }) {
                    it[deviceLastSync] = time
                }
            }

            AuthCheck.Ok(user[UsersTable.role], user[UsersTable.siteId])
        }

    companion object {
        const val SESSION_REVOKED = "Session revoked"
        const val DEVICE_REVOKED = "Device revoked"
        const val TOUCH_INTERVAL_MS = 15 * 60 * 1000L
    }
}
