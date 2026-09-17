package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "animal_movements")
data class AnimalMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val animalGuid: String,
    val fromLocation: String,
    val toLocation: String,
    val timestamp: Long

)