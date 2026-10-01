package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_security_events",
    indices = [
        Index(
            value = ["event_key"],
            unique = true
        ),
        Index(value = ["user_id"]),
        Index(value = ["event_time"])
    ]
)
data class SyncSecurityEvent(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "event_key")
    val eventKey: String,

    @ColumnInfo(name = "user_id")
    val userId: String?,

    @ColumnInfo(name = "event_type")
    val eventType: String,

    @ColumnInfo(name = "event_time")
    val eventTime: Long,

    @ColumnInfo(name = "warning_day")
    val warningDay: Int? = null,

    @ColumnInfo(name = "pending_count")
    val pendingCount: Int,

    @ColumnInfo(name = "oldest_pending_created_at")
    val oldestPendingCreatedAt: Long? = null,

    val details: String? = null
)
