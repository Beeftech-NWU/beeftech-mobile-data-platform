package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_backups",
    foreignKeys = [
        ForeignKey(
            entity = SyncBatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["batchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["batchId"])
    ]
)
data class SyncBackupEntity(
    @PrimaryKey
    val id: String,
    val batchId: String,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "PENDING"
)
