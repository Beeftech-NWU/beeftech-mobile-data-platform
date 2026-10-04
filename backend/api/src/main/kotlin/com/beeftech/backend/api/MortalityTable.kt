package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Table

/* New table, so SchemaUtils.create makes it on existing databases; no ALTER migration needed. */
object MortalityTable : Table("mortalities") {

    val id = long("id").autoIncrement()

    val animalId = varchar("animal_id", 255).index()
    val causeOfDeath = text("cause_of_death")
    val necropsyCodeId = varchar("necropsy_code_id", 255).nullable()
    val responsibleWorker = varchar("responsible_worker", 255)
    val notes = text("notes").nullable()
    val timestamp = long("timestamp")

    val deviceId = varchar("device_id", 255)
    val recordguid = varchar("recordguid", 255).uniqueIndex()

    val syncStatus = varchar("sync_status", 50)
    val syncedAt = long("synced_at").nullable()

    val submittedByUserId = varchar("submitted_by_user_id", 64).nullable()
    val siteId = varchar("site_id", 64).nullable()

    /* A voided record is kept but hidden from lists and counts. See VoidRepository. */
    val voidedAt = long("voided_at").nullable()
    val voidedByUserId = varchar("voided_by_user_id", 64).nullable()
    val voidReason = text("void_reason").nullable()

    override val primaryKey = PrimaryKey(id)
}
