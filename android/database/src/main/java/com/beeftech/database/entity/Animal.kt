package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "animals",
    indices = [
        Index(value = ["tagNumber"]),
        Index(value = ["temperatureNumber"]),
        Index(value = ["damId"]),
        Index(value = ["sireId"]),
        Index(value = ["animalGroupId"])
    ]
)
data class Animal(
    @PrimaryKey
    val animalId: String,

    val tagNumber: String? = null,
    val oldTagNumber: String? = null,

    val temperatureNumber: String? = null,
    val referenceNumber: String? = null,
    val massKg: Double? = null,
    val birthdate: Long,
    val breed: String,
    val gender: String? = null,
    val age: Int? = null,
    val condition: String? = null,
    val hideColour: String? = null,
    val earMarking: String? = null,
    val brandMark: String? = null,

    val damId: String? = null,
    val sireId: String? = null,
    val animalGroupId: String? = null,

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
