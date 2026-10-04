package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Table

/* New table, so SchemaUtils.create makes it on existing databases; no ALTER migration needed. */
object AuditLogTable : Table("audit_log") {

    val id = long("id").autoIncrement()

    /* What was done: see AuditActions. */
    val action = varchar("action", 50)

    /* A VoidTarget.entityType (or USER) and the record's own id (record GUID, farmer id, or user id). */
    val entityType = varchar("entity_type", 50).index()
    val entityId = varchar("entity_id", 255)

    /* Empty when the action takes no reason (only a void does). */
    val reason = text("reason")

    /* A JSON object of what changed, e.g. {"role":"3->2"}. Never holds a PIN or hash. */
    val details = text("details").nullable()

    val actorUserId = varchar("actor_user_id", 64)
    val actorUsername = varchar("actor_username", 255)
    val actorRole = integer("actor_role")

    /* The affected record's site, so a manager can read the log for their own site. */
    val siteId = varchar("site_id", 64).nullable().index()

    val createdAt = long("created_at").index()

    override val primaryKey = PrimaryKey(id)
}
