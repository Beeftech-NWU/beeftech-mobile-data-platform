package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "animal_movements",
    indices = [
        Index(
            value = ["recordguid"],
            unique = true
        )
    ]
)
data class AnimalMovement(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val animalId: String,

    val movementType: String,

    @ColumnInfo(defaultValue = "''")
    val responsibleWorker: String = "",

    val timestamp: Long,

    @ColumnInfo(defaultValue = "0.0")
    val gpsLat: Double = 0.0,

    @ColumnInfo(defaultValue = "0.0")
    val gpsLng: Double = 0.0,

    @ColumnInfo(defaultValue = "''")
    val deviceId: String = "",

    @ColumnInfo(defaultValue = "''")
    val recordguid: String =
        UUID.randomUUID().toString(),

    @ColumnInfo(defaultValue = "'PENDING'")
    val syncStatus: String = "PENDING",

    val syncedAt: Long? = null
)