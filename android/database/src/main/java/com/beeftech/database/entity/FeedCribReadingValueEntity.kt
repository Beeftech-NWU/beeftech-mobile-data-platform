package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "feed_crib_reading_values",
    foreignKeys = [
        ForeignKey(
            entity = FeedCribReadingEntity::class,
            parentColumns = ["id"],
            childColumns = ["readingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["readingId"]),
        Index(value = ["record_guid"], unique = true)
    ]
)
data class FeedCribReadingValueEntity(
    @PrimaryKey
    val id: String,
    val readingId: String,
    val value: Double,
    @ColumnInfo(defaultValue = "''")
    val record_guid: String = UUID.randomUUID().toString()
)
