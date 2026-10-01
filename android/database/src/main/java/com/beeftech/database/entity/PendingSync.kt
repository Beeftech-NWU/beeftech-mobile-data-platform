package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync")
data class PendingSync(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /*
     * Nullable for records created before database version 31.
     *
     * Legacy NULL rows are deliberately NOT attributed to a
     * user, because guessing ownership could lock or wipe the
     * wrong person's data.
     */
    @ColumnInfo(name = "user_id")
    val userId: String? = null,

    val entityType: String,
    val entityId: String,
    val operation: String,
    val payload: String,
    val createdAt: Long,
    val retryCount: Int = 0
)
