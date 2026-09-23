package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "animal_ownerships",
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
        Index(value = ["owner_name"]),
        Index(value = ["start_date"])
    ]
)
data class AnimalOwnershipEntity(
    @PrimaryKey
    @ColumnInfo(name = "ownership_id")
    val ownershipId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "animal_id")
    val animalId: String,

    @ColumnInfo(name = "owner_name")
    val ownerName: String,

    @ColumnInfo(name = "ownership_percentage")
    val ownershipPercentage: Double = 100.0,

    @ColumnInfo(name = "start_date")
    val startDate: String,

    @ColumnInfo(name = "end_date")
    val endDate: String? = null
)
