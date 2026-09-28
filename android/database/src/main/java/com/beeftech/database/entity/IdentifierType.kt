package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "identifier_types")
data class IdentifierType(
    @PrimaryKey
    val code: String,
    val name: String,
    @ColumnInfo(name = "validation_regex")
    val validationRegex: String? = null
)
