package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "farmers")
data class FarmerEntity(
    @PrimaryKey
    val farmer_id: String,
    val client_code: String?,
    val organisation_name: String?,
    val vat_number: String?,
    val email_address: String?,
    val gps_latitude: Double?,
    val gps_longitude: Double?,
    val sync_status: String = "PENDING"
)