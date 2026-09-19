package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "animal_group_memberships",
    primaryKeys = ["animalId", "groupId", "dateJoined"],
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
data class AnimalGroupMembership(
    val animalId: String,
    val groupId: String,
    // Unix timestamps in milliseconds.
    val dateJoined: Long,
    val dateLeft: Long? = null
)
