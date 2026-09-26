package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Lookup of cost categories (remediation plan, Phase 5).
 *
 * animal_costs.costType is a foreign key to [code], so adding
 * a new cost category is an INSERT here, never a migration.
 */
@Entity(tableName = "cost_types")
data class CostType(
    @PrimaryKey
    val code: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "is_active", defaultValue = "1")
    val isActive: Boolean = true
)
