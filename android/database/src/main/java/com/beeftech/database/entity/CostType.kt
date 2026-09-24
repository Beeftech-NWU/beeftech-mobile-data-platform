package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data-driven cost categories. Adding a category means inserting a row here,
 * not adding a column to animal_costs.
 */
@Entity(tableName = "cost_types")
data class CostType(
    @PrimaryKey
    val code: String,
    val label: String
)
