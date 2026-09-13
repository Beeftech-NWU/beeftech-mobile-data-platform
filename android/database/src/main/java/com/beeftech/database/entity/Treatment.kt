package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "treatments")
data class Treatment(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val animalId: String,

    val disease: String,

    val treatmentName: String,

    val batchNumber: String,

    val volumeUsed: String,

    val cost: Double,

    val timestamp: Long
)