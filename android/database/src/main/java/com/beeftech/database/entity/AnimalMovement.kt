package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "animal_movements",
    indices = [
        Index(value = ["record_guid"], unique = true)
    ]
)
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
    val timestamp: Long,

    @ColumnInfo(name = "record_guid")
    val recordGuid: String = UUID.randomUUID().toString()
)
