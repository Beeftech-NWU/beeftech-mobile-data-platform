package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "calf_registrations",
    foreignKeys = [
        ForeignKey(
            entity = Animal::class,
            parentColumns = ["animalId"],
            childColumns = ["registered_animal_id"],
            onDelete = ForeignKey.CASCADE
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
        Index(value = ["registered_animalId"], unique = true),
        Index(value = ["dam_id"]),
        Index(value = ["sire_id"])
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

