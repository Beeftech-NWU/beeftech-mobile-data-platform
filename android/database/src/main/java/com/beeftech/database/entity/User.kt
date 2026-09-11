package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "users",
    foreignKeys = [
        ForeignKey(
            entity = Role::class,
            parentColumns = ["role_id"],
            childColumns = ["role"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["role"])
    ]
)
data class User(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "username")
    val username: String,

    @ColumnInfo(name = "pin_hash")
    val pinHash: String? = null,

    @ColumnInfo(name = "role")
    val role: Long? = null,

    @ColumnInfo(name = "device_assigned_id")
    val deviceAssignedId: String? = null,

    @ColumnInfo(name = "device_last_sync")
    val deviceLastSync: Long? = null,

    @ColumnInfo(name = "failed_sync_attempts")
    val failedSyncAttempts: Int = 0
)
