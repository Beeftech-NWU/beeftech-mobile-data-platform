package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.UUID

@Entity(
    tableName = "animals",
    foreignKeys = [
        ForeignKey(
            entity = Breed::class,
            parentColumns = ["code"],
            childColumns = ["breed"],
            onDelete = ForeignKey.RESTRICT),
        ForeignKey(
            entity = HideColour::class,
            parentColumns = ["code"],
            childColumns = ["hideColour"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [
        Index(value = ["tagNumber"]),
        Index(value = ["temperatureNumber"]),
        Index(value = ["parentId"]),
        Index(value = ["animalGroupId"]),
        Index(value = ["breed"]),
        Index(value = ["hideColour"]),
        Index(value = ["recordguid"], unique = true)

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
    val brandMark: String? = null,

    val parentId: String? = null,
    val animalGroupId: String? = null,

    val photoPath: String? = null,
    val videoPath: String? = null,

    val gpsLat: Double,
    val gpsLng: Double,
    val captureAt: Long,
    val deviceId: String,
    val recordguid: String = UUID.randomUUID().toString(),

    val syncStatus: String = "PENDING",
    val syncedat: Long? = null
)
