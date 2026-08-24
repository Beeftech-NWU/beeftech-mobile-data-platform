package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "treatments")
data class Treatment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val animalId: String,
    val treatmentType: String,
    val medication: String?,
    val notes: String?,
    val timestamp: Long
)