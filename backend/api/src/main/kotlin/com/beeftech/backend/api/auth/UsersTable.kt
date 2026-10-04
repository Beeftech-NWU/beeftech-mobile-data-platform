package com.beeftech.backend.api.auth

import org.jetbrains.exposed.sql.Table

object UsersTable : Table("users") {
    val userId = varchar("user_id", 64)
    val username = varchar("username", 255).uniqueIndex()
    val pinHash = varchar("pin_hash", 72)
    val role = integer("role").nullable()
    val deviceAssignedId = varchar("device_assigned_id", 255).nullable()
    val deviceLastSync = long("device_last_sync").nullable()
    val failedSyncAttempts = integer("failed_sync_attempts").default(0)
    val siteId = varchar("site_id", 64).nullable()
    val active = bool("active").default(true)

    /*
     * Tokens issued before this time (epoch ms) are rejected. Set when an account is
     * deactivated, its phone unbound or its PIN reset, so those take effect at once.
     */
    val tokensValidAfter = long("tokens_valid_after").nullable()

    /*
     * When an admin last cleared this user's Day-7 sync lock (epoch ms). The phone compares it
     * with when it locked and lifts its own lock if this is newer.
     */
    val syncLockClearedAt = long("sync_lock_cleared_at").nullable()

    override val primaryKey = PrimaryKey(userId)
}
