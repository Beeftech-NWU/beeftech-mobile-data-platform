package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed_cribs")
data class FeedCribEntity(
    @PrimaryKey
    val id: String,
    val name: String? = null
)
