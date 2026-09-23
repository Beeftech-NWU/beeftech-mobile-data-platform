package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "animal_identifiers",
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
        Index(value = ["identifier_type", "identifier_value"])
    ]
)
data class AnimalIdentifierEntity(
    @PrimaryKey
    @ColumnInfo(name = "identifier_id")
    val identifierId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "animal_id")
    val animalId: String,

    @ColumnInfo(name = "identifier_type")
    val identifierType: String,

    @ColumnInfo(name = "identifier_value")
    val identifierValue: String,

    @ColumnInfo(name = "valid_from")
    val validFrom: String? = null,

    @ColumnInfo(name = "valid_to")
    val validTo: String? = null
)
