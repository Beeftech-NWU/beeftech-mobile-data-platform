package com.beeftech.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.beeftech.database.dao.AnimalMovementDao
import com.beeftech.database.entity.AnimalMovement
import com.beeftech.database.dao.TreatmentDao
import com.beeftech.database.entity.Treatment
import com.beeftech.database.dao.MortalityDao
import com.beeftech.database.entity.Mortality
import com.beeftech.database.dao.PendingSyncDao
import com.beeftech.database.entity.PendingSync
import com.beeftech.database.entity.CalfRegistration
import com.beeftech.database.dao.CalfRegistrationDao

@Database(
    entities = [AnimalMovement::class,
               Treatment::class,
               Mortality::class,
               PendingSync::class,
               CalfRegistration::class
               ],
    version = 1,
    exportSchema = false
)
abstract class BeefTechDatabase : RoomDatabase() {

    abstract fun animalMovementDao(): AnimalMovementDao
    abstract fun treatmentDao(): TreatmentDao
    abstract fun mortalityDao(): MortalityDao
    abstract fun pendingSyncDao(): PendingSyncDao
    abstract fun calfRegistrationDao(): CalfRegistrationDao
}