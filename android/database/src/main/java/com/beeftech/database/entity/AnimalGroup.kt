package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "animal_groups",
    indices = [
        Index(value = ["groupName"], unique = true)
    ]
)
data class AnimalGroup(
    @PrimaryKey
    val animalGroupId: String,

    val groupName: String,

    val description: String? = null
)
