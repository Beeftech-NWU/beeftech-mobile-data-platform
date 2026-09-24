package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "animal_costs",
    indices = [
        Index(value = ["animalId", "costType", "timestamp"]),
        Index(value = ["source_entity", "source_record_id"], unique = true),
        Index(value = ["record_guid"], unique = true)
    ]
)
data class AnimalCost(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val animalId: String,

    /** A row-level category. New categories do not require a schema change. */
    val costType: String,

    val amount: Double,

    @ColumnInfo(defaultValue = "''")
    val description: String = "",

    val gpsLat: Double = 0.0,

    val gpsLng: Double = 0.0,

    val timestamp: Long,

    @ColumnInfo(name = "source_entity")
    val sourceEntity: String? = null,

    @ColumnInfo(name = "source_record_id")
    val sourceRecordId: String? = null,

    @ColumnInfo(name = "record_guid")
    val recordGuid: String = UUID.randomUUID().toString()
)
