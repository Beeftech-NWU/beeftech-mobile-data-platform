package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "animal_purchases",
    foreignKeys = [
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["animalId"],
            childColumns = ["animal_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["animal_id"]),
        Index(value = ["purchase_date"])
    ]
)
data class AnimalPurchaseEntity(
    @PrimaryKey
    @ColumnInfo(name = "purchase_id")
    val purchaseId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "animal_id")
    val animalId: String,

    @ColumnInfo(name = "purchase_price")
    val purchasePrice: Double,

    @ColumnInfo(name = "purchase_date")
    val purchaseDate: String,

    @ColumnInfo(name = "seller_name")
    val sellerName: String, // Replaces Supplier entity

    @ColumnInfo(name = "notes")
    val notes: String? = null
) {
    val supplierName: String get() = sellerName
}
