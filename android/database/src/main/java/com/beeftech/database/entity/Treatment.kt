package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "treatments",
    foreignKeys = [
        ForeignKey(
            entity = Animal::class,
            parentColumns = ["animalId"],
            childColumns = ["animalId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Disease::class,
            parentColumns = ["diseaseId"],
            childColumns = ["disease"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Device::class,
            parentColumns = ["deviceId"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["animalId"]),
        Index(value = ["disease"]),
        Index(value = ["deviceId"]),
        Index(
            value = ["record_guid"],
            unique = true
        )
    ]
)
data class Treatment(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val animalId: String,

    val disease: String,

    val treatmentName: String,

    val batchNumber: String,

    val volumeUsed: String,

    /** Mirrored into animal_costs by TreatmentDao.insertWithCost. */
    val cost: Double,

    val gpsLat: Double,

    val gpsLng: Double,

    val timestamp: Long,

    @ColumnInfo(defaultValue = "''")
    val deviceId: String = "",

    @ColumnInfo(name = "withdrawal_clear_date")
    val withdrawalClearDate: Long? = null,

    @ColumnInfo(name = "record_guid", defaultValue = "''")
    val recordGuid: String =
        UUID.randomUUID().toString(),

    @ColumnInfo(defaultValue = "'PENDING'")
    val syncStatus: String = "PENDING",

    val syncedAt: Long? = null
)
