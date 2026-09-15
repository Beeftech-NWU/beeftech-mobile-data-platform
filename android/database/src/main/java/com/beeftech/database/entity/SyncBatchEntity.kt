package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_batches")
data class SyncBatchEntity(
    @PrimaryKey
    val id: String,
    val timestamp: Long
)
