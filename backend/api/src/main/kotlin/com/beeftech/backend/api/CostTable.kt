package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Table

/* New table, so SchemaUtils.create makes it on existing databases; no ALTER migration needed. */
object CostTable : Table("animal_costs") {

    val id = long("id").autoIncrement()

    val animalId = varchar("animal_id", 255).index()
    val costType = varchar("cost_type", 100)
    val amount = double("amount")
    val description = text("description")
    val gpsLat = double("gps_lat")
    val gpsLng = double("gps_lng")
    val timestamp = long("timestamp")

    /* A cost derived from another record names its source; manual costs leave both null. */
    val sourceEntity = varchar("source_entity", 50).nullable()
    val sourceRecordId = varchar("source_record_id", 255).nullable()

    val deviceId = varchar("device_id", 255)
    val recordguid = varchar("recordguid", 255).uniqueIndex()

    val syncStatus = varchar("sync_status", 50)
    val syncedAt = long("synced_at").nullable()

    val submittedByUserId = varchar("submitted_by_user_id", 64).nullable()
    val siteId = varchar("site_id", 64).nullable()

    override val primaryKey = PrimaryKey(id)
}
