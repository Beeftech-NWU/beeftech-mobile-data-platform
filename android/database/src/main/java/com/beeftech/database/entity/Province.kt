package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "provinces",
    foreignKeys = [
        ForeignKey(
            entity = Country::class,
            parentColumns = ["countryId"],
            childColumns = ["countryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["countryId"])
    ]
)
data class Province(
    @PrimaryKey
    val provinceId: String,
    val countryId: String,
    val name: String
)
