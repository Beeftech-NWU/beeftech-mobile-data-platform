package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "feed_crib_readings",
    foreignKeys = [
        ForeignKey(
            entity = FeedCribEntity::class,
            parentColumns = ["id"],
            childColumns = ["cribId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["cribId"]),
        Index(value = ["record_guid"], unique = true)
    ]
)
data class FeedCribReadingEntity(
    @PrimaryKey
    val id: String,
    val cribId: String,
    @ColumnInfo(defaultValue = "''")
    val record_guid: String = UUID.randomUUID().toString()
)
