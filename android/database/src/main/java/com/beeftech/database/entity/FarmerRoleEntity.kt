package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "farmer_roles")
data class FarmerRoleEntity(
    @PrimaryKey
    val farmer_role_id: String,
    val farmer_id: String,
    val role_id: String
)