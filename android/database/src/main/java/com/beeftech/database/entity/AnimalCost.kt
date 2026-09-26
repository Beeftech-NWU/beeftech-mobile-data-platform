package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * One cost incurred against an animal, as a row per cost
 * (remediation plan, Phase 5). Totals are never stored; they are
 * always computed from these rows.
 */
@Entity(
    tableName = "animal_costs",
    foreignKeys = [
        ForeignKey(
            entity = CostType::class,
            parentColumns = ["code"],
            childColumns = ["costType"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["record_guid"], unique = true),
        Index(value = ["costType"]),
        Index(value = ["animalId", "costType", "timestamp"]),
        // One derived cost per source record, so deriving is idempotent.
        Index(value = ["source_entity", "source_record_id"], unique = true)
    ]
)
data class AnimalCost(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val animalId: String,

    /** A [CostType.code]. */
    val costType: String,

    val amount: Double,

    @ColumnInfo(defaultValue = "''")
    val description: String = "",

    val gpsLat: Double,

    val gpsLng: Double,

    val timestamp: Long,

    /** One of [CostSource], or null for a manually captured cost. */
    @ColumnInfo(name = "source_entity")
    val sourceEntity: String? = null,

    /** The source row's record GUID, or null for a manually captured cost. */
    @ColumnInfo(name = "source_record_id")
    val sourceRecordId: String? = null,

    @ColumnInfo(name = "record_guid")
    val recordGuid: String = UUID.randomUUID().toString()
)
