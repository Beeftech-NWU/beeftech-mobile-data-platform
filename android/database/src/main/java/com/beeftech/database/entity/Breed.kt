package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "breeds"
)
data class Breed(
    @PrimaryKey
    val breedId: String,
    val name: String
)
