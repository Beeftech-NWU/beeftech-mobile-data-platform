package com.beeftech.backend.api.auth

import com.beeftech.backend.api.AuditEntry
import com.beeftech.backend.api.DatabaseFactory
import com.beeftech.backend.api.insertAuditRow
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

data class LoginAttemptState(val failedAttempts: Int, val lockedUntil: Long?)

class LoginSecurityRepository {

    suspend fun state(username: String): LoginAttemptState? =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            LoginAttemptsTable.selectAll()
                .where { LoginAttemptsTable.username eq username }
                .singleOrNull()
                ?.let { LoginAttemptState(it[LoginAttemptsTable.failedAttempts], it[LoginAttemptsTable.lockedUntil]) }
        }

    /*
     * Counts a failed attempt and locks the username once it reaches [maxAttempts]. A lock that
     * has run out starts the count again, so the user gets a fresh set of attempts.
     */
    suspend fun recordFailure(username: String, now: Long, maxAttempts: Int, lockMs: Long): LoginAttemptState =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            val row = LoginAttemptsTable.selectAll().where { LoginAttemptsTable.username eq username }.singleOrNull()
            val expired = row?.get(LoginAttemptsTable.lockedUntil)?.let { it <= now } ?: false
            val failed = (if (row == null || expired) 0 else row[LoginAttemptsTable.failedAttempts]) + 1
            val lockedUntil = if (failed >= maxAttempts) now + lockMs else null

            if (row == null) {
                LoginAttemptsTable.insert {
                    it[LoginAttemptsTable.username] = username
                    it[failedAttempts] = failed
                    it[LoginAttemptsTable.lockedUntil] = lockedUntil
                    it[updatedAt] = now
                }
            } else {
                LoginAttemptsTable.update({ LoginAttemptsTable.username eq username }) {
                    it[failedAttempts] = failed
                    it[LoginAttemptsTable.lockedUntil] = lockedUntil
                    it[updatedAt] = now
                }
            }
            LoginAttemptState(failed, lockedUntil)
        }

    suspend fun clear(username: String, audit: AuditEntry? = null) {
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            audit?.let { insertAuditRow(it) }
            LoginAttemptsTable.deleteWhere { LoginAttemptsTable.username eq username }
        }
    }

    /* Usernames that are locked out right now, longest-locked first. */
    suspend fun lockouts(now: Long): List<LockoutDto> =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            LoginAttemptsTable.selectAll()
                .where { LoginAttemptsTable.lockedUntil greater now }
                .orderBy(LoginAttemptsTable.lockedUntil, SortOrder.DESC)
                .map {
                    LockoutDto(
                        username = it[LoginAttemptsTable.username],
                        failedAttempts = it[LoginAttemptsTable.failedAttempts],
                        lockedUntil = it[LoginAttemptsTable.lockedUntil]!!
                    )
                }
        }

    /* Newest first by id, so "before" pages stably. All filters optional. */
    suspend fun events(siteId: String?, userId: String?, outcome: String?, before: Long?, limit: Int): List<LoginEventDto> =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            var where: Op<Boolean> = Op.TRUE
            siteId?.let { where = where and (LoginEventsTable.siteId eq it) }
            userId?.let { where = where and (LoginEventsTable.userId eq it) }
            outcome?.let { where = where and (LoginEventsTable.outcome eq it) }
            before?.let { where = where and (LoginEventsTable.id less it) }

            LoginEventsTable.selectAll()
                .where { where }
                .orderBy(LoginEventsTable.id, SortOrder.DESC)
                .limit(limit)
                .map {
                    LoginEventDto(
                        id = it[LoginEventsTable.id],
                        createdAt = it[LoginEventsTable.createdAt],
                        usernameAttempted = it[LoginEventsTable.usernameAttempted],
                        userId = it[LoginEventsTable.userId],
                        deviceId = it[LoginEventsTable.deviceId],
                        outcome = it[LoginEventsTable.outcome],
                        siteId = it[LoginEventsTable.siteId],
                        appVersion = it[LoginEventsTable.appVersion]
                    )
                }
        }

    suspend fun recordEvent(
        usernameAttempted: String,
        userId: String?,
        deviceId: String,
        outcome: String,
        siteId: String?,
        appVersion: String?,
        now: Long
    ) {
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            LoginEventsTable.insert {
                it[createdAt] = now
                it[LoginEventsTable.usernameAttempted] = usernameAttempted
                it[LoginEventsTable.userId] = userId
                it[LoginEventsTable.deviceId] = deviceId
                it[LoginEventsTable.outcome] = outcome
                it[LoginEventsTable.siteId] = siteId
                it[LoginEventsTable.appVersion] = appVersion
            }
        }
    }
}
