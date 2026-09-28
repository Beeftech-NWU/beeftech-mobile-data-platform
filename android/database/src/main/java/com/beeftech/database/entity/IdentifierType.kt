package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "identifier_types")
data class IdentifierType(
    @PrimaryKey
    val code: String,
    val name: String,
    val validationRegex: String? = null
)
