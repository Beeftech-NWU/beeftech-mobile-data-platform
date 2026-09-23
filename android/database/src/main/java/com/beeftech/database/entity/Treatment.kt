package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "treatments",
    indices = [
        Index(value = ["record_guid"], unique = true)
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

    @ColumnInfo(name = "record_guid")
    val recordGuid: String = UUID.randomUUID().toString()
)
