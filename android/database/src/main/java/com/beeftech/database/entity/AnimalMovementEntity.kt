package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "animal_movements",
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
        Index(value = ["destination_farm_id"]),
        Index(value = ["destination_pen_id"])
    ]
)
data class AnimalMovementEntity(
    @PrimaryKey
    @ColumnInfo(name = "movement_id")
    val movementId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "animal_id")
    val animalId: String,

    @ColumnInfo(name = "source_farm_id")
    val sourceFarmId: String? = null,

    @ColumnInfo(name = "source_pen_id")
    val sourcePenId: String? = null,

    @ColumnInfo(name = "destination_farm_id")
    val destinationFarmId: String,

    @ColumnInfo(name = "destination_pen_id")
    val destinationPenId: String,

    @ColumnInfo(name = "movement_date")
    val movementDate: String,

    // Folded from LocationFeed
    @ColumnInfo(name = "feed_location_type")
    val feedLocationType: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null
) {
    val movementType: String get() = destinationFarmId
    val responsibleWorker: String get() = notes ?: ""
    val timestamp: Long get() = movementDate.toLongOrNull() ?: 0L
}
