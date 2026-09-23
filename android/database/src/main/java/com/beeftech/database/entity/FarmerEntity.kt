package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "farmers",
    indices = [
        Index(value = ["record_guid"], unique = true)
    ]
)
data class FarmerEntity(
    @PrimaryKey
    val farmer_id: String,
    val client_code: String?,
    val organisation_name: String?,
    val vat_number: String?,
    val email_address: String?,
    val gps_latitude: Double?,
    val gps_longitude: Double?,
    val sync_status: String = "PENDING",

    @ColumnInfo(name = "record_guid")
    val recordGuid: String = UUID.randomUUID().toString()
)
