package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pens")
data class PenEntity(
    @PrimaryKey
    val id: String,
    val name: String? = null
)
