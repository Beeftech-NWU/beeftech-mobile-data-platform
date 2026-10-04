package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "mortalities",
    foreignKeys = [
        ForeignKey(
            entity = Animal::class,
            parentColumns = ["animalId"],
            childColumns = ["animalId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NecropsyCode::class,
            parentColumns = ["necropsyCodeId"],
            childColumns = ["necropsy_code_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["animalId"], unique = true),
        Index(value = ["necropsy_code_id"]),
        Index(value = ["record_guid"], unique = true)
    ]
)
data class Mortality(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val animalId: String,

    val causeOfDeath: String,

    @ColumnInfo(name = "necropsy_code_id")
    val necropsyCodeId: String? = null,

    @ColumnInfo(defaultValue = "''")
    val responsibleWorker: String = "",

    val notes: String? = null,

    val timestamp: Long,

    @ColumnInfo(name = "record_guid")
    val recordGuid: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "sync_status", defaultValue = "'PENDING'")
    val syncStatus: String = "PENDING",

    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null
)
