package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Table

/* New table, so SchemaUtils.create makes it on existing databases; no ALTER migration needed. */
object AuditLogTable : Table("audit_log") {

    val id = long("id").autoIncrement()

    /* What was done, e.g. VOID. */
    val action = varchar("action", 50)

    /* A VoidTarget.entityType and the record's own id (record GUID, or farmer id). */
    val entityType = varchar("entity_type", 50).index()
    val entityId = varchar("entity_id", 255)

    val reason = text("reason")

    val actorUserId = varchar("actor_user_id", 64)
    val actorUsername = varchar("actor_username", 255)
    val actorRole = integer("actor_role")

    /* The affected record's site, so a manager can read the log for their own site. */
    val siteId = varchar("site_id", 64).nullable().index()

    val createdAt = long("created_at").index()

    override val primaryKey = PrimaryKey(id)
}
