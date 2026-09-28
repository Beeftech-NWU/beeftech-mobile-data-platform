package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "hide_colours"
)
data class HideColour(
    @PrimaryKey
    val colourId: String,
    val name: String
)
