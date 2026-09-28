package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "animals",
    foreignKeys = [
        ForeignKey(
            entity = AnimalGroup::class,
            parentColumns = ["animalGroupId"],
            childColumns = ["animalGroupId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Breed::class,
            parentColumns = ["breedId"],
            childColumns = ["breed"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = HideColour::class,
            parentColumns = ["colourId"],
            childColumns = ["hideColour"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Device::class,
            parentColumns = ["deviceId"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["tagNumber"]),
        Index(value = ["temperatureNumber"]),
        Index(value = ["parentId"]),
        Index(value = ["animalGroupId"]),
        Index(value = ["breed"]),
        Index(value = ["hideColour"]),
        Index(value = ["deviceId"]),
        Index(value = ["record_guid"], unique = true)
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

    @ColumnInfo(name = "record_guid")
    val recordGuid: String = UUID.randomUUID().toString(),

    val syncStatus: String = "PENDING",
    val syncedat: Long? = null
)
