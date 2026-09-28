package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_batches",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["medicationId"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["medicationId"]),
        Index(value = ["batch_number"])
    ]
)
data class MedicationBatch(
    @PrimaryKey
    val batchId: String,
    val medicationId: String,
    @ColumnInfo(name = "batch_number")
    val batchNumber: String,
    @ColumnInfo(name = "expiry_date")
    val expiryDate: Long? = null
)
