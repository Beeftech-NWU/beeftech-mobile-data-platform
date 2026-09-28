package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "devices"
)
data class Device(
    @PrimaryKey
    val deviceId: String,
    @ColumnInfo(name = "device_assigned_id")
    val deviceAssignedId: String? = null,
    val model: String? = null,
    @ColumnInfo(name = "last_sync")
    val lastSync: Long? = null
)
