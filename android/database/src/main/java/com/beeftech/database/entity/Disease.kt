package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diseases")
data class Disease(
    @PrimaryKey
    val code: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "is_active", defaultValue = "1")
    val isActive: Boolean = true
)
