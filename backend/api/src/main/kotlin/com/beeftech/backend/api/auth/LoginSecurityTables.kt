package com.beeftech.backend.api.auth

import org.jetbrains.exposed.sql.Table

/*
 * New tables, so SchemaUtils.create makes them on existing databases; no ALTER migration needed.
 */

/* One row per phone that has signed in. A REVOKED phone can't sign in and its tokens stop working. */
object DevicesTable : Table("devices") {
    val deviceId = varchar("device_id", 255)
    val model = varchar("model", 255).nullable()
    val appVersion = varchar("app_version", 64).nullable()
    val status = varchar("status", 16).default(DeviceStatus.ACTIVE)
    val firstSeenAt = long("first_seen_at")
    val lastSeenAt = long("last_seen_at")
    val lastUserId = varchar("last_user_id", 64).nullable()
    val siteId = varchar("site_id", 64).nullable().index()
    val revokedAt = long("revoked_at").nullable()
    val revokedByUserId = varchar("revoked_by_user_id", 64).nullable()
    val revokeReason = text("revoke_reason").nullable()

    override val primaryKey = PrimaryKey(deviceId)
}

object DeviceStatus {
    const val ACTIVE = "ACTIVE"
    const val REVOKED = "REVOKED"
}

/* Failed-login counters, kept in the database so a restart doesn't clear a lockout. */
object LoginAttemptsTable : Table("login_attempts") {
    val username = varchar("username", 255)
    val failedAttempts = integer("failed_attempts")
    val lockedUntil = long("locked_until").nullable()
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(username)
}

/* Every login attempt and its outcome. Never holds a PIN. */
object LoginEventsTable : Table("login_events") {
    val id = long("id").autoIncrement()
    val createdAt = long("created_at").index()

    /* Whatever was typed, including names that don't exist. */
    val usernameAttempted = varchar("username_attempted", 255)
    val userId = varchar("user_id", 64).nullable()
    val deviceId = varchar("device_id", 255)
    val outcome = varchar("outcome", 32)
    val siteId = varchar("site_id", 64).nullable().index()
    val appVersion = varchar("app_version", 64).nullable()

    override val primaryKey = PrimaryKey(id)
}

object LoginOutcome {
    const val SUCCESS = "SUCCESS"
    const val BAD_CREDENTIALS = "BAD_CREDENTIALS"
    const val UNKNOWN_USER = "UNKNOWN_USER"
    const val LOCKED = "LOCKED"
    const val INACTIVE = "INACTIVE"
    const val WRONG_DEVICE = "WRONG_DEVICE"
    const val DEVICE_REVOKED = "DEVICE_REVOKED"
}
