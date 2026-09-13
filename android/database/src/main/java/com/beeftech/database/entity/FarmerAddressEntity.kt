package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "farmer_addresses")
data class FarmerAddressEntity(
    @PrimaryKey
    val address_id: String,
    val farmer_id: String,
    val address_type: String?,
    val address_line_1: String?,
    val province: String?,
    val postal_code: String?,
    val gps_latitude: Double?,
    val gps_longitude: Double?
)