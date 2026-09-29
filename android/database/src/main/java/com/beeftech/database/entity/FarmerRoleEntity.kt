package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "farmer_roles",
    foreignKeys = [
        ForeignKey(
            entity = FarmerEntity::class,
            parentColumns = ["farmer_id"],
            childColumns = ["farmer_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FarmerBusinessRole::class,
            parentColumns = ["business_role_id"],
            childColumns = ["role_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["farmer_id"]),
        Index(value = ["role_id"]),
        Index(value = ["farmer_id", "role_id"], unique = true)
    ]
)
data class FarmerRoleEntity(
    @PrimaryKey
    val farmer_role_id: String,
    val farmer_id: String,
    val role_id: Long
)
