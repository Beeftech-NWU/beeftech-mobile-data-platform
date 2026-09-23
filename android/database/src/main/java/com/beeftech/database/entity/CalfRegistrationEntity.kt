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
            entity = AnimalEntity::class,
            parentColumns = ["animalId"],
            childColumns = ["registered_animal_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["animalId"],
            childColumns = ["dam_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["animalId"],
            childColumns = ["sire_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["registered_animal_id"]),
        Index(value = ["dam_id"]),
        Index(value = ["sire_id"])
    ]
)
data class CalfRegistrationEntity(
    @PrimaryKey
    @ColumnInfo(name = "registration_id")
    val registrationId: String = UUID.randomUUID().toString(),

    // Keyed explicitly to the canonical primary animal entity
    @ColumnInfo(name = "registered_animal_id")
    val registeredAnimalId: String,

    @ColumnInfo(name = "dam_id")
    val damId: String? = null,

    @ColumnInfo(name = "sire_id")
    val sireId: String? = null,

    @ColumnInfo(name = "birth_weight_kg")
    val birthWeightKg: Double? = null,

    @ColumnInfo(name = "calving_ease")
    val calvingEase: String? = null,

    @ColumnInfo(name = "registration_date")
    val registrationDate: String
)
