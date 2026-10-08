package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Table

object CalfRegistrationTable : Table("calf_registrations") {

    val id = integer("id").autoIncrement()

    val tagNumber = varchar("tag_number", 255).uniqueIndex()
    val animalUuid = varchar("animal_uuid", 64).nullable().uniqueIndex()
    val birthdate = long("birthdate")
    val breed = varchar("breed", 255)
    val gender = varchar("gender", 32).nullable()
    val hideColour = varchar("hide_colour", 64).nullable()
    val brandMark = varchar("brand_mark", 255).nullable()
    val birthWeightKg = double("birth_weight_kg").nullable()
    val ageClass = varchar("age_class", 64).nullable()
    val bodyCondition = varchar("body_condition", 64).nullable()
    val conformity = varchar("conformity", 64).nullable()
    val oldTagNumber = varchar("old_tag_number", 255).nullable()
    val referenceNumber = varchar("reference_number", 255).nullable()
    val processProof = varchar("process_proof", 512).nullable()
    val implantProof = varchar("implant_proof", 512).nullable()
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

    /* A voided record is kept but hidden from lists and counts. See VoidRepository. */
    val voidedAt = long("voided_at").nullable()
    val voidedByUserId = varchar("voided_by_user_id", 64).nullable()
    val voidReason = text("void_reason").nullable()

    override val primaryKey = PrimaryKey(id)
}
