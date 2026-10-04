package com.beeftech.backend.api

import com.beeftech.backend.api.auth.UsersTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class SyncSecurityRepository {

    /*
     * Stores the events in one transaction. Re-sent events are skipped (insertIgnore on the
     * unique (device, key) index) and counted as duplicates.
     */
    suspend fun insertAll(
        deviceId: String,
        userId: String,
        username: String,
        events: List<SyncSecurityEventUpload>,
        now: Long
    ): Int =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            /* The site is whatever the user's site is now, not what any client said. */
            val siteId = UsersTable.selectAll()
                .where { UsersTable.userId eq userId }
                .singleOrNull()?.get(UsersTable.siteId)

            events.count { event ->
                val inserted = SyncSecurityEventsTable.insertIgnore {
                    it[SyncSecurityEventsTable.deviceId] = deviceId
                    it[eventKey] = event.eventKey
                    it[SyncSecurityEventsTable.userId] = userId
                    it[SyncSecurityEventsTable.username] = username
                    it[SyncSecurityEventsTable.siteId] = siteId
                    it[eventType] = event.eventType
                    it[eventTime] = event.eventTime
                    it[warningDay] = event.warningDay
                    it[pendingCount] = event.pendingCount
                    it[oldestPendingCreatedAt] = event.oldestPendingCreatedAt
                    it[details] = event.details
                    it[receivedAt] = now
                }
                inserted.insertedCount > 0
            }
        }

    /* Newest first. Page with before = the id of the last event you have. */
    suspend fun list(
        siteId: String?,
        userId: String?,
        eventType: String?,
        before: Long?,
        limit: Int
    ): List<SyncSecurityEventDto> =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            var where: Op<Boolean> = Op.TRUE
            if (siteId != null) where = where and (SyncSecurityEventsTable.siteId eq siteId)
            if (userId != null) where = where and (SyncSecurityEventsTable.userId eq userId)
            if (eventType != null) where = where and (SyncSecurityEventsTable.eventType eq eventType)
            if (before != null) where = where and (SyncSecurityEventsTable.id less before)

            SyncSecurityEventsTable.selectAll()
                .where { where }
                .orderBy(SyncSecurityEventsTable.id, SortOrder.DESC)
                .limit(limit)
                .map { it.toDto() }
        }

    /*
     * Users whose latest reported lock is newer than the last time an admin cleared it.
     * Clearing a lock moves users.sync_lock_cleared_at, so the account drops off this list at
     * once, and a later lock (a newer event) puts it back.
     */
    suspend fun locked(siteId: String?): List<LockedAccountDto> =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            val users = UsersTable.selectAll()
                .where { if (siteId != null) UsersTable.siteId eq siteId else Op.TRUE }
                .associateBy { it[UsersTable.userId] }

            SyncSecurityEventsTable.selectAll()
                .where { SyncSecurityEventsTable.eventType eq SyncSecurityEventTypes.ACCOUNT_LOCKED }
                .orderBy(SyncSecurityEventsTable.eventTime, SortOrder.DESC)
                .toList()
                .groupBy { it[SyncSecurityEventsTable.userId] }
                .mapNotNull { (userId, rows) ->
                    val user = users[userId] ?: return@mapNotNull null
                    val latest = rows.first()
                    val cleared = user[UsersTable.syncLockClearedAt]
                    if (cleared != null && latest[SyncSecurityEventsTable.eventTime] <= cleared) {
                        return@mapNotNull null
                    }
                    LockedAccountDto(
                        userId = userId,
                        username = user[UsersTable.username],
                        siteId = user[UsersTable.siteId],
                        deviceId = latest[SyncSecurityEventsTable.deviceId],
                        lockedAt = latest[SyncSecurityEventsTable.eventTime],
                        reason = latest[SyncSecurityEventsTable.details]
                    )
                }
                .sortedByDescending { it.lockedAt }
        }

    private fun ResultRow.toDto() = SyncSecurityEventDto(
        id = this[SyncSecurityEventsTable.id],
        deviceId = this[SyncSecurityEventsTable.deviceId],
        userId = this[SyncSecurityEventsTable.userId],
        username = this[SyncSecurityEventsTable.username],
        siteId = this[SyncSecurityEventsTable.siteId],
        eventType = this[SyncSecurityEventsTable.eventType],
        eventTime = this[SyncSecurityEventsTable.eventTime],
        warningDay = this[SyncSecurityEventsTable.warningDay],
        pendingCount = this[SyncSecurityEventsTable.pendingCount],
        oldestPendingCreatedAt = this[SyncSecurityEventsTable.oldestPendingCreatedAt],
        details = this[SyncSecurityEventsTable.details],
        receivedAt = this[SyncSecurityEventsTable.receivedAt]
    )
}
