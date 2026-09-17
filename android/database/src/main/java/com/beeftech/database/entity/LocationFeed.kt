package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_feed")
data class LocationFeed(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val animalId: String,

    val destination: String,

    val daysInDestination: Int,

    val rationName: String,

    val rationDays: Int,

    val rationCost: Double,

    val timestamp: Long
)