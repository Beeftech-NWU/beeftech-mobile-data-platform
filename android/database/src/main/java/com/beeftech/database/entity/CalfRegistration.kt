package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "calf_registrations",
    indices = [
        Index(value = ["animalId"], unique = true),
        Index(value = ["recordguid"], unique = true)
    ]
)
data class CalfRegistration(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val animalId: String,
    val birthdate: Long,
    val breed: String,
    val damId: String? = null,
    val sireId: String? = null,

    val photoPath: String? = null,
    val videoPath: String? = null,

    val gpsLat: Double,
    val gpsLng: Double,
    val captureAt: Long,
    val deviceId: String,
    val recordguid: String,

    val syncStatus: String = "PENDING",
    val syncedat: Long? = null
)

