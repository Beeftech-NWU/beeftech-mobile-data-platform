package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "necropsy_codes"
)
data class NecropsyCode(
    @PrimaryKey
    val necropsyCodeId: String,
    val code: String,
    val description: String
)
