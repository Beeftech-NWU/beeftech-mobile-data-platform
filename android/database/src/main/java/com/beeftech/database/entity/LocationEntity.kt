package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey
    val location_id: String,
    val location_code: String?,
    val location_name: String?,
    val location_type: String?
)