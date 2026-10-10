package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** A bunk score (0-5) a worker can pick for a reading. Downloaded with the cribs. */
@Entity(tableName = "crib_reading_codes")
data class CribReadingCodeEntity(
    @PrimaryKey
    val code: Int,

    val label: String,

    @ColumnInfo(defaultValue = "''")
    val description: String = "",

    @ColumnInfo(defaultValue = "1")
    val active: Boolean = true
)
