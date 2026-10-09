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
    val co_reg_id_no: String? = null,
    val land_ownership: String? = null,
    val fa_code_rmis: String? = null,
    val gln_number: String? = null,
    val herd_capacity: Int? = null,
    val interest_status: String? = null,
    val contact_name: String? = null,
    val contact_number: String? = null,
    val farm_size_ha: Double? = null,
    val head_count: Int? = null,
    val primary_breed: String? = null,

    @ColumnInfo(name = "record_guid")
    val recordGuid: String = UUID.randomUUID().toString()
)
