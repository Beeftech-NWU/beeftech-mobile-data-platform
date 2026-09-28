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
            entity = Animal::class,
            parentColumns = ["animalId"],
            childColumns = ["animal_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["animal_id"]),
        Index(value = ["purchase_date"]),
        Index(value = ["record_guid"], unique = true)
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
    val purchaseDate: Long,

    @ColumnInfo(name = "seller_name")
    val sellerName: String, // Replaces Supplier entity

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "record_guid", defaultValue = "''")
    val recordGuid: String = UUID.randomUUID().toString()
) {
    val supplierName: String get() = sellerName
}
