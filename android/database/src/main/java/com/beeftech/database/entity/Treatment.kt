package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "treatments",
    indices = [
        Index(
            value = ["recordguid"],
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

    val cost: Double,

    val timestamp: Long,

    @ColumnInfo(defaultValue = "''")
    val deviceId: String = "",

    @ColumnInfo(defaultValue = "''")
    val recordguid: String =
        UUID.randomUUID().toString(),

    @ColumnInfo(defaultValue = "'PENDING'")
    val syncStatus: String = "PENDING",

    val syncedAt: Long? = null
)