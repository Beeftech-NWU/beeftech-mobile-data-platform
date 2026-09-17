package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Table

object CalfRegistrationTable : Table("calf_registrations") {

    val id = integer("id").autoIncrement()

    val animalId = varchar("animal_id", 255).uniqueIndex()
    val birthdate = long("birthdate")
    val breed = varchar("breed", 255)
    val damId = varchar("dam_id", 255).nullable()
    val sireId = varchar("sire_id", 255).nullable()

    val photoPath = varchar("photo_path", 512).nullable()
    val videoPath = varchar("video_path", 512).nullable()

    val gpsLat = double("gps_lat")
    val gpsLng = double("gps_lng")
    val captureAt = long("capture_at")
    val deviceId = varchar("device_id", 255)
    val recordguid = varchar("recordguid", 255).uniqueIndex()

    val syncStatus = varchar("sync_status", 32).default("PENDING")
    val syncedAt = long("synced_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
