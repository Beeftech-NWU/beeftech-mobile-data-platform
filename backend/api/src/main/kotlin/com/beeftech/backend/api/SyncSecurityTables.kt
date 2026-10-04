package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Table

/*
 * New table, so SchemaUtils.create makes it on existing databases; no ALTER migration needed.
 *
 * What the phones recorded about unsynced data: warnings, the Day-7 wipe and the account lock.
 * A phone sends each event once (event_key is made on the phone), and re-sending is harmless:
 * (device_id, event_key) is unique.
 */
object SyncSecurityEventsTable : Table("sync_security_events") {
    val id = long("id").autoIncrement()
    val deviceId = varchar("device_id", 255)
    val eventKey = varchar("event_key", 255)

    /* From the token, never from the request. */
    val userId = varchar("user_id", 64).index()
    val username = varchar("username", 255)

    /* Read from the database when the event arrives, not from the request or the token. */
    val siteId = varchar("site_id", 64).nullable().index()
    val eventType = varchar("event_type", 64)
    val eventTime = long("event_time").index()
    val warningDay = integer("warning_day").nullable()
    val pendingCount = integer("pending_count")
    val oldestPendingCreatedAt = long("oldest_pending_created_at").nullable()
    val details = text("details").nullable()
    val receivedAt = long("received_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("sync_security_events_device_key", deviceId, eventKey)
    }
}

/* The phone's event types that mean the account is locked on the phone until an admin clears it. */
object SyncSecurityEventTypes {
    const val ACCOUNT_LOCKED = "SYNC_POLICY_ACCOUNT_LOCKED"
}
