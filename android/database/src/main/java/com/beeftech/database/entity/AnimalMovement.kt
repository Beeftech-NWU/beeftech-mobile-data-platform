package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "animal_movements")
data class AnimalMovement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val animalId: String,
    val movementType: String,
    val timestamp: Long
)