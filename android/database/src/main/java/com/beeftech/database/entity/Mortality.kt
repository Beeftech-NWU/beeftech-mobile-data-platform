package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "mortalities",
    indices = [
        Index(value = ["record_guid"], unique = true)
    ]
)
data class Mortality(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val animalId: String,

    val causeOfDeath: String,

    @ColumnInfo(defaultValue = "''")
    val responsibleWorker: String = "",

    val notes: String? = null,

    val timestamp: Long,

    @ColumnInfo(name = "record_guid")
    val recordGuid: String = UUID.randomUUID().toString()
)
