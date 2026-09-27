package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "animal_group_memberships",
    foreignKeys = [
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["animalId"],
            childColumns = ["animal_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AnimalGroup::class,
            parentColumns = ["animalGroupId"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["animal_id"]),
        Index(value = ["group_id"]),
        Index(value = ["record_guid"], unique = true)
    ]
)
data class AnimalGroupMembershipEntity(
    @PrimaryKey
    @ColumnInfo(name = "membership_id")
    val membershipId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "animal_id")
    val animalId: String,

    @ColumnInfo(name = "group_id")
    val groupId: String,

    @ColumnInfo(name = "joined_at")
    val joinedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "left_at")
    val leftAt: Long? = null,

    @ColumnInfo(name = "record_guid")
    val recordGuid: String = UUID.randomUUID().toString()
)
