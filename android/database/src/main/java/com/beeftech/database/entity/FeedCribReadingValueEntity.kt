package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed_crib_reading_values")
data class FeedCribReadingValueEntity(
    @PrimaryKey
    val id: String,
    val readingId: String,
    val value: Double
)
