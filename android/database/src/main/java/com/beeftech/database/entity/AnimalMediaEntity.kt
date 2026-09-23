package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "animal_media",
    foreignKeys = [
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["animalId"],
            childColumns = ["animal_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["animal_id"])
    ]
)
data class AnimalMediaEntity(
    @PrimaryKey
    @ColumnInfo(name = "media_id")
    val mediaId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "animal_id")
    val animalId: String,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "media_type")
    val mediaType: String, // e.g., "PHOTO", "VIDEO"

    @ColumnInfo(name = "created_at")
    val createdAt: String
)
