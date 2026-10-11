package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Table

object AnimalMovementTable :
    Table("animal_movements") {

    val id =
        long("id")
            .autoIncrement()

    val animalId =
        varchar(
            "animal_id",
            255
        )

    val movementType =
        text(
            "movement_type"
        )

    val responsibleWorker =
        varchar(
            "responsible_worker",
            255
        )

    val timestamp =
        long(
            "timestamp"
        )

    val gpsLat =
        double(
            "gps_lat"
        )

    val gpsLng =
        double(
            "gps_lng"
        )

    val deviceId =
        varchar(
            "device_id",
            255
        )

    val recordguid =
        varchar(
            "recordguid",
            255
        )
            .uniqueIndex()

    val syncStatus =
        varchar(
            "sync_status",
            50
        )

    val syncedAt =
        long(
            "synced_at"
        )
            .nullable()

    override val primaryKey =
        PrimaryKey(id)
}