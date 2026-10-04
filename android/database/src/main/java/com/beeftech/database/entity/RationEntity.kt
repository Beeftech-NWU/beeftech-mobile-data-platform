package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rations",
    indices = [
        Index(
            value = ["name"],
            unique = true
        )
    ]
)
data class RationEntity(

    @PrimaryKey
    @ColumnInfo(name = "ration_id")
    val rationId: String,

    val name: String,

    @ColumnInfo(defaultValue = "1")
    val active: Boolean = true
)
