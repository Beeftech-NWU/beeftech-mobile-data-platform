package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "animal_costs")
data class AnimalCost(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val animalId: String,

    val costType: String,

    val amount: Double,

    @ColumnInfo(defaultValue = "''")
    val description: String = "",

    val timestamp: Long
)