package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "farmer_addresses",
    foreignKeys = [
        ForeignKey(
            entity = FarmerEntity::class,
            parentColumns = ["farmer_id"],
            childColumns = ["farmer_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["farmer_id"]),
        Index(value = ["record_guid"], unique = true)
    ]
)
data class FarmerAddressEntity(
    @PrimaryKey
    val address_id: String,
    val farmer_id: String,
    val address_type: String?,
    val address_line_1: String?,
    val province: String?,
    val postal_code: String?,
    val gps_latitude: Double?,
    val gps_longitude: Double?,
    val record_guid: String = UUID.randomUUID().toString()
)