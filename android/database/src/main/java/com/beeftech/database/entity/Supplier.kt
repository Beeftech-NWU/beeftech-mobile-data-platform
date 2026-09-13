package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suppliers")
data class Supplier(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val animalId: String,

    val supplierName: String,

    val glnNumber: String,

    val purchaseDate: String,

    val purchaseBatchNumber: String,

    val timestamp: Long
)