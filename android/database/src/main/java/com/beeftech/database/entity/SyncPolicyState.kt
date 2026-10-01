package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_policy_state"
)
data class SyncPolicyState(

    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(
        name = "locked",
        defaultValue = "0"
    )
    val locked: Boolean = false,

    @ColumnInfo(name = "locked_at")
    val lockedAt: Long? = null,

    @ColumnInfo(name = "lock_reason")
    val lockReason: String? = null
)
