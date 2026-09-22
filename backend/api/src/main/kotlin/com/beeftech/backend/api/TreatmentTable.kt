package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Table

object TreatmentTable : Table("treatments") {

    val id = integer("id").autoIncrement()

    val animalId = varchar("animal_id", 255)
    val disease = varchar("disease", 255)
    val treatmentName = varchar("treatment_name", 255)
    val batchNumber = varchar("batch_number", 255)
    val volumeUsed = varchar("volume_used", 255)
    val cost = double("cost")
    val timestamp = long("timestamp")

    val deviceId = varchar("device_id", 255)
    val recordguid = varchar("recordguid", 255).uniqueIndex()

    val syncStatus = varchar("sync_status", 32).default("PENDING")
    val syncedAt = long("synced_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
