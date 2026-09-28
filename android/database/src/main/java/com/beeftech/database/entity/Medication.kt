package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "medications"
)
data class Medication(
    @PrimaryKey
    val medicationId: String,
    val name: String,
    @ColumnInfo(name = "withdrawal_period_days", defaultValue = "0")
    val withdrawalPeriodDays: Int = 0
)
