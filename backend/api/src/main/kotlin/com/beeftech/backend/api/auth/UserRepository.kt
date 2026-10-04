package com.beeftech.backend.api.auth

import com.beeftech.backend.api.AuditEntry
import com.beeftech.backend.api.DatabaseFactory
import com.beeftech.backend.api.insertAuditRow
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

data class UserRecord(
    val userId: String,
    val username: String,
    val pinHash: String,
    val role: Int?,
    val deviceAssignedId: String?,
    val deviceLastSync: Long?,
    val failedSyncAttempts: Int,
    val siteId: String? = null,
    val active: Boolean = true
)

class UserRepository {

    suspend fun findByUsername(username: String): UserRecord? {
        return newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            UsersTable.selectAll()
                .where { UsersTable.username eq username }
                .map { it.toUserRecord() }
                .singleOrNull()
        }
    }

    suspend fun claimDevice(userId: String, deviceId: String) {
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            UsersTable.update({ UsersTable.userId eq userId }) {
                it[deviceAssignedId] = deviceId
            }
        }
    }

    suspend fun touchLastSync(userId: String) {
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            UsersTable.update({ UsersTable.userId eq userId }) {
                it[deviceLastSync] = System.currentTimeMillis()
            }
        }
    }

    suspend fun insertUser(
        userId: String,
        username: String,
        pinHash: String,
        role: Int?,
        deviceAssignedId: String? = null,
        siteId: String? = null,
        active: Boolean = true,
        audit: AuditEntry? = null
    ) {
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            audit?.let { insertAuditRow(it) }
            UsersTable.insert {
                it[UsersTable.userId] = userId
                it[UsersTable.username] = username
                it[UsersTable.pinHash] = pinHash
                it[UsersTable.role] = role
                it[UsersTable.deviceAssignedId] = deviceAssignedId
                it[UsersTable.siteId] = siteId
                it[UsersTable.active] = active
            }
        }
    }

    suspend fun findById(userId: String): UserRecord? {
        return newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            UsersTable.selectAll()
                .where { UsersTable.userId eq userId }
                .map { it.toUserRecord() }
                .singleOrNull()
        }
    }

    suspend fun list(siteId: String?, role: Int?): List<UserRecord> {
        return newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            var filter: Op<Boolean> = Op.TRUE
            if (siteId != null) filter = filter and (UsersTable.siteId eq siteId)
            if (role != null) filter = filter and (UsersTable.role eq role)
            UsersTable.selectAll()
                .where { filter }
                .orderBy(UsersTable.username)
                .map { it.toUserRecord() }
        }
    }

    suspend fun updateAccount(
        userId: String,
        role: Int?,
        siteId: String?,
        active: Boolean,
        audit: AuditEntry? = null
    ) {
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            audit?.let { insertAuditRow(it) }
            UsersTable.update({ UsersTable.userId eq userId }) {
                it[UsersTable.role] = role
                it[UsersTable.siteId] = siteId
                it[UsersTable.active] = active
            }
        }
    }

    suspend fun updatePinHash(userId: String, pinHash: String, audit: AuditEntry? = null) {
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            audit?.let { insertAuditRow(it) }
            UsersTable.update({ UsersTable.userId eq userId }) {
                it[UsersTable.pinHash] = pinHash
            }
        }
    }

    suspend fun clearDevice(userId: String, audit: AuditEntry? = null) {
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            audit?.let { insertAuditRow(it) }
            UsersTable.update({ UsersTable.userId eq userId }) {
                it[deviceAssignedId] = null
            }
        }
    }

    suspend fun siteExists(siteId: String): Boolean {
        return newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            SitesTable.selectAll().where { SitesTable.siteId eq siteId }.any()
        }
    }

    /* Null when the site doesn't exist, otherwise its active flag. */
    suspend fun siteActive(siteId: String): Boolean? {
        return newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            SitesTable.selectAll()
                .where { SitesTable.siteId eq siteId }
                .singleOrNull()
                ?.get(SitesTable.active)
        }
    }

    suspend fun updateSite(userId: String, siteId: String?) {
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            UsersTable.update({ UsersTable.userId eq userId }) {
                it[UsersTable.siteId] = siteId
            }
        }
    }

    private fun ResultRow.toUserRecord(): UserRecord {
        return UserRecord(
            userId = this[UsersTable.userId],
            username = this[UsersTable.username],
            pinHash = this[UsersTable.pinHash],
            role = this[UsersTable.role],
            deviceAssignedId = this[UsersTable.deviceAssignedId],
            deviceLastSync = this[UsersTable.deviceLastSync],
            failedSyncAttempts = this[UsersTable.failedSyncAttempts],
            siteId = this[UsersTable.siteId],
            active = this[UsersTable.active]
        )
    }
}
