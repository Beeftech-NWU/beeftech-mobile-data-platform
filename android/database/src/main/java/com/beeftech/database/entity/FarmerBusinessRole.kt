package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "farmer_business_roles",
    indices = [
        Index(value = ["business_role_name"], unique = true)
    ]
)
data class FarmerBusinessRole(
    @PrimaryKey
    @ColumnInfo(name = "business_role_id")
    val businessRoleId: Long,

    @ColumnInfo(name = "business_role_name")
    val businessRoleName: String
)
