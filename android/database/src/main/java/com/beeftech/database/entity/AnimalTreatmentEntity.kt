package com.beeftech.database.entity
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "animal_treatments")
data class AnimalTreatmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val animalGuid: String,
    val treatmentType: String,
    val dosage: String,
    val timestamp: Long
)