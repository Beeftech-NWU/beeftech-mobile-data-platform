package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "animal_weights",
    foreignKeys = [
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["animalId"],
            childColumns = ["animal_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["animal_id"]),
        Index(value = ["weigh_date"])
    ]
)
data class AnimalWeightEntity(
    @PrimaryKey
    @ColumnInfo(name = "weight_id")
    val weightId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "animal_id")
    val animalId: String,

    @ColumnInfo(name = "weight_kg")
    val weightKg: Double,

    @ColumnInfo(name = "weigh_date")
    val weighDate: String,

    @ColumnInfo(name = "notes")
    val notes: String? = null
)
