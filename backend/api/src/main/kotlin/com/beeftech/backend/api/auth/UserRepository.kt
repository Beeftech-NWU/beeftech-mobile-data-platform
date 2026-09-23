package com.beeftech.backend.api.auth

import com.beeftech.backend.api.DatabaseFactory
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
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
    val failedSyncAttempts: Int
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
        deviceAssignedId: String? = null
    ) {
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            UsersTable.insert {
                it[UsersTable.userId] = userId
                it[UsersTable.username] = username
                it[UsersTable.pinHash] = pinHash
                it[UsersTable.role] = role
                it[UsersTable.deviceAssignedId] = deviceAssignedId
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
            failedSyncAttempts = this[UsersTable.failedSyncAttempts]
        )
    }
}
