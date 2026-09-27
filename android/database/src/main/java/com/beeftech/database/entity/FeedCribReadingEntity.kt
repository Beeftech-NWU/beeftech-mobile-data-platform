package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
        Index(value = ["cribId"])
    ]
)
data class FeedCribReadingEntity(
    @PrimaryKey
    val id: String,
    val cribId: String
)
