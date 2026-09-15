package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed_crib_readings")
data class FeedCribReadingEntity(
    @PrimaryKey
    val id: String,
    val cribId: String
)
