package com.beeftech.backend.api.auth

import com.beeftech.backend.api.AuditEntry
import com.beeftech.backend.api.DatabaseFactory
import com.beeftech.backend.api.insertAuditRow
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class DeviceRepository {

    /*
     * Records that [deviceId] signed in. The model and version are kept from an earlier login
     * when this one doesn't send them (an older app).
     */
    suspend fun recordLogin(
        deviceId: String,
        model: String?,
        appVersion: String?,
        userId: String,
        siteId: String?,
        now: Long
    ) {
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            val exists = DevicesTable.selectAll().where { DevicesTable.deviceId eq deviceId }.any()
            if (exists) {
                DevicesTable.update({ DevicesTable.deviceId eq deviceId }) {
                    if (model != null) it[DevicesTable.model] = model
                    if (appVersion != null) it[DevicesTable.appVersion] = appVersion
                    it[lastSeenAt] = now
                    it[lastUserId] = userId
                    it[DevicesTable.siteId] = siteId
                }
            } else {
                DevicesTable.insert {
                    it[DevicesTable.deviceId] = deviceId
                    it[DevicesTable.model] = model
                    it[DevicesTable.appVersion] = appVersion
                    it[firstSeenAt] = now
                    it[lastSeenAt] = now
                    it[lastUserId] = userId
                    it[DevicesTable.siteId] = siteId
                }
            }
        }
    }

    /* Newest contact first. [siteId] narrows to one site; [status] to ACTIVE or REVOKED. */
    suspend fun list(siteId: String?, status: String?): List<DeviceDto> =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            val bound = boundUsernames()
            val usernames = UsersTable.selectAll().associate { it[UsersTable.userId] to it[UsersTable.username] }
            DevicesTable.selectAll()
                .apply {
                    if (siteId != null) andWhere { DevicesTable.siteId eq siteId }
                    if (status != null) andWhere { DevicesTable.status eq status }
                }
                .orderBy(DevicesTable.lastSeenAt, SortOrder.DESC)
                .map { it.toDto(bound[it[DevicesTable.deviceId]].orEmpty(), usernames) }
        }

    suspend fun find(deviceId: String): DeviceDto? =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            val usernames = UsersTable.selectAll().associate { it[UsersTable.userId] to it[UsersTable.username] }
            DevicesTable.selectAll()
                .where { DevicesTable.deviceId eq deviceId }
                .singleOrNull()
                ?.toDto(boundUsernames()[deviceId].orEmpty(), usernames)
        }

    suspend fun revoke(deviceId: String, actorUserId: String, reason: String, now: Long, audit: AuditEntry) {
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            insertAuditRow(audit, now)
            DevicesTable.update({ DevicesTable.deviceId eq deviceId }) {
                it[status] = DeviceStatus.REVOKED
                it[revokedAt] = now
                it[revokedByUserId] = actorUserId
                it[revokeReason] = reason
            }
        }
    }

    suspend fun reinstate(deviceId: String, audit: AuditEntry, now: Long) {
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            insertAuditRow(audit, now)
            /* The earlier revoke stays in the audit log; the row only carries the current state. */
            DevicesTable.update({ DevicesTable.deviceId eq deviceId }) {
                it[status] = DeviceStatus.ACTIVE
                it[revokedAt] = null
                it[revokedByUserId] = null
                it[revokeReason] = null
            }
        }
    }

    private fun boundUsernames(): Map<String, List<String>> =
        UsersTable.selectAll()
            .filter { it[UsersTable.deviceAssignedId] != null }
            .groupBy({ it[UsersTable.deviceAssignedId]!! }, { it[UsersTable.username] })
            .mapValues { it.value.sorted() }

    private fun ResultRow.toDto(bound: List<String>, usernames: Map<String, String>) = DeviceDto(
        deviceId = this[DevicesTable.deviceId],
        model = this[DevicesTable.model],
        appVersion = this[DevicesTable.appVersion],
        status = this[DevicesTable.status],
        firstSeenAt = this[DevicesTable.firstSeenAt],
        lastSeenAt = this[DevicesTable.lastSeenAt],
        lastUserId = this[DevicesTable.lastUserId],
        lastUsername = this[DevicesTable.lastUserId]?.let { usernames[it] },
        siteId = this[DevicesTable.siteId],
        revokedAt = this[DevicesTable.revokedAt],
        revokedByUserId = this[DevicesTable.revokedByUserId],
        revokeReason = this[DevicesTable.revokeReason],
        boundUsernames = bound
    )

    suspend fun isRevoked(deviceId: String): Boolean =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            DevicesTable.selectAll()
                .where { DevicesTable.deviceId eq deviceId }
                .singleOrNull()
                ?.get(DevicesTable.status) == DeviceStatus.REVOKED
        }
}
