package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_logs")
data class SyncLog(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "batch_size")
    val batchSize: Int,

    @ColumnInfo(name = "status")
    val status: String, // SUCCESS, FAILED, RETRY

    @ColumnInfo(name = "endpoint")
    val endpoint: String,

    @ColumnInfo(name = "response_message")
    val responseMessage: String,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long = 0
)
