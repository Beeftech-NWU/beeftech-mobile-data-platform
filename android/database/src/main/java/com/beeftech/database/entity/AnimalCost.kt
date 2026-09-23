package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "animal_costs",
    indices = [
        Index(value = ["record_guid"], unique = true)
    ]
)
data class AnimalCost(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val animalId: String,

    val costType: String,

    val amount: Double,

    @ColumnInfo(defaultValue = "''")
    val description: String = "",

    val gpsLat: Double,

    val gpsLng: Double,

    val timestamp: Long,

    @ColumnInfo(name = "record_guid")
    val recordGuid: String = UUID.randomUUID().toString()
)
