package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mortalities")
data class Mortality(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val animalId: String,
    val causeOfDeath: String,
    val notes: String?,
    val timestamp: Long
)