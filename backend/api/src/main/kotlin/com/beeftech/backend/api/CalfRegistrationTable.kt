package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Table

object CalfRegistrationTable : Table("calf_registrations") {

    val id = integer("id").autoIncrement()

    val tagNumber = varchar("tag_number", 255).uniqueIndex()
    val animalUuid = varchar("animal_uuid", 64).nullable().uniqueIndex()
    val birthdate = long("birthdate")
    val breed = varchar("breed", 255)
    val damTagNumber = varchar("dam_tag_number", 255).nullable()
    val sireTagNumber = varchar("sire_tag_number", 255).nullable()
    val damAnimalUuid = varchar("dam_animal_uuid", 64).nullable()
    val sireAnimalUuid = varchar("sire_animal_uuid", 64).nullable()

    val photoPath = varchar("photo_path", 512).nullable()
    val videoPath = varchar("video_path", 512).nullable()

    val gpsLat = double("gps_lat")
    val gpsLng = double("gps_lng")
    val captureAt = long("capture_at")
    val deviceId = varchar("device_id", 255)
    val recordguid = varchar("recordguid", 255).uniqueIndex()

    val syncStatus = varchar("sync_status", 32).default("PENDING")
    val syncedAt = long("synced_at").nullable()

    val submittedByUserId = varchar("submitted_by_user_id", 64).nullable()
    val siteId = varchar("site_id", 64).nullable()

    override val primaryKey = PrimaryKey(id)
}
