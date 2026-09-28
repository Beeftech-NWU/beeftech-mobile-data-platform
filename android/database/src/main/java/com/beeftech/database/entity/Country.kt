package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "countries"
)
data class Country(
    @PrimaryKey
    val countryId: String,
    @ColumnInfo(name = "iso_code")
    val isoCode: String,
    val name: String
)
