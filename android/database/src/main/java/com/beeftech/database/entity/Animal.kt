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
        ),
        ForeignKey(
            entity = Animal::class,
            parentColumns = ["animalId"],
            childColumns = ["dam_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Animal::class,
            parentColumns = ["animalId"],
            childColumns = ["sire_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["dam_id"]),
        Index(value = ["sire_id"]),
        Index(value = ["breed"]),
        Index(value = ["hideColour"]),
        Index(value = ["deviceId"]),
        Index(value = ["record_guid"], unique = true)
    ]
)
data class Animal(
    @PrimaryKey
    val animalId: String,

    val birthdate: Long,
    val breed: String,
    val gender: String? = null,
    val hideColour: String? = null,
    val brandMark: String? = null,

    @ColumnInfo(name = "dam_id")
    val damId: String? = null,

    @ColumnInfo(name = "sire_id")
    val sireId: String? = null,

    val gpsLat: Double,
    val gpsLng: Double,
    val captureAt: Long,
    val deviceId: String,

    @ColumnInfo(name = "record_guid")
    val recordGuid: String = UUID.randomUUID().toString(),

    val syncStatus: String = "PENDING",
    val syncedat: Long? = null
)
