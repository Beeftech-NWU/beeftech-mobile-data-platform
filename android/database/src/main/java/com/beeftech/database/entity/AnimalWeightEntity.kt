package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "animal_weights",
    foreignKeys = [
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["animalId"],
            childColumns = ["animal_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["animal_id"]),
        Index(value = ["weigh_date"]),
        Index(value = ["record_guid"], unique = true)
    ]
)
data class AnimalWeightEntity(
    @PrimaryKey
    @ColumnInfo(name = "weight_id")
    val weightId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "animal_id")
    val animalId: String,

    @ColumnInfo(name = "weight_kg")
    val weightKg: Double,

    @ColumnInfo(name = "weigh_date")
    val weighDate: String,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "record_guid", defaultValue = "''")
    val recordGuid: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "gps_lat", defaultValue = "0.0")
    val gpsLat: Double = 0.0,

    @ColumnInfo(name = "gps_lng", defaultValue = "0.0")
    val gpsLng: Double = 0.0,

    @ColumnInfo(name = "device_id", defaultValue = "''")
    val deviceId: String = "",

    @ColumnInfo(name = "captured_at", defaultValue = "0")
    val capturedAt: Long = 0L,

    @ColumnInfo(name = "sync_status", defaultValue = "'PENDING'")
    val syncStatus: String = "PENDING",

    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null
)
