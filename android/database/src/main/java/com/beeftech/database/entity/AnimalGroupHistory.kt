package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "animal_group_history",
    primaryKeys = ["animalId", "groupId", "dateChange"],
    foreignKeys = [
        ForeignKey(
            entity = Animal::class,
            parentColumns = ["animalId"],
            childColumns = ["animalId"]
        ),
        ForeignKey(
            entity = AnimalGroup::class,
            parentColumns = ["animalGroupId"],
            childColumns = ["groupId"]
        )
    ],
    indices = [Index(value = ["groupId"])]
)
data class AnimalGroupHistory(
    val animalId: String,
    val groupId: String,
    // Unix timestamp in milliseconds.
    val dateChange: Long
)
