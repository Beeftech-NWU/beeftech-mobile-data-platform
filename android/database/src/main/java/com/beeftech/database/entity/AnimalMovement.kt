package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "animal_movements")
data class AnimalMovement(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val animalId: String,

    // Stores the movement information entered on the screen
    val movementType: String,

    // Worker responsible for the movement
    @ColumnInfo(defaultValue = "''")
    val responsibleWorker: String = "",

    // Time the record was saved
    val timestamp: Long
)