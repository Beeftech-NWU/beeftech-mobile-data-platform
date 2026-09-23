package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "animal_weights",
    indices = [
        Index(value = ["record_guid"], unique = true)
    ]
)
data class AnimalWeightEntity(
    @PrimaryKey
    @ColumnInfo(name = "weight_id")
    val weightId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "animal_id")
    val animalId: String, // Uses tag_num identifier only per strategy

    @ColumnInfo(name = "mass_kg")
    val massKg: Double,

    @ColumnInfo(name = "body_condition_score")
    val bodyConditionScore: Double? = null,

    @ColumnInfo(name = "weighed_at")
    val weighedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "record_guid")
    val recordGuid: String = UUID.randomUUID().toString()
)
