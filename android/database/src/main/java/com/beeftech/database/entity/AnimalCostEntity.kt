package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "animal_costs")
data class AnimalCostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val animalGuid: String,
    val costType: String,
    val amount: Double,
    val timeStamp: Long
)