package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "animal_mortalities")
data class AnimalMortalityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val animalGuid: String,
    val cause: String,
    val timestamp: Long
)