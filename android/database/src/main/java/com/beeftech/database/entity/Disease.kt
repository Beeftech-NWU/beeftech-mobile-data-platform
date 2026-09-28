package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "diseases"
)
data class Disease(
    @PrimaryKey
    val diseaseId: String,
    val name: String
)
