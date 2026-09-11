package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "roles",
    indices = [
        Index(value = ["role_name"], unique = true)
    ]
)
data class Role(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "role_id")
    val roleId: Long = 0,

    @ColumnInfo(name = "role_name")
    val roleName: String
)
