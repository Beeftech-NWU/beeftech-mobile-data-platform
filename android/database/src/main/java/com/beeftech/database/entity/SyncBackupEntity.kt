package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_backups")
data class SyncBackupEntity(
    @PrimaryKey
    val id: String,
    val batchId: String,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "PENDING"
)
