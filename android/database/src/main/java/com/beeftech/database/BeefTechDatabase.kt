package com.beeftech.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.beeftech.database.dao.AnimalCostDao
import com.beeftech.database.dao.AnimalMovementDao
import com.beeftech.database.dao.CalfRegistrationDao
import com.beeftech.database.dao.LocationFeedDao
import com.beeftech.database.dao.MortalityDao
import com.beeftech.database.dao.PendingSyncDao
import com.beeftech.database.dao.SupplierDao
import com.beeftech.database.dao.TreatmentDao
import com.beeftech.database.entity.AnimalCost
import com.beeftech.database.entity.AnimalMovement
import com.beeftech.database.entity.CalfRegistration
import com.beeftech.database.entity.LocationFeed
import com.beeftech.database.entity.Mortality
import com.beeftech.database.entity.PendingSync
import com.beeftech.database.entity.Supplier
import com.beeftech.database.entity.Treatment

@Database(
    entities = [
        AnimalMovement::class,
        Treatment::class,
        Mortality::class,
        PendingSync::class,
        CalfRegistration::class,
        Supplier::class,
        LocationFeed::class,
        AnimalCost::class
    ],
    version = 7,
    exportSchema = false
)
abstract class BeefTechDatabase : RoomDatabase() {

    abstract fun animalMovementDao(): AnimalMovementDao

    abstract fun treatmentDao(): TreatmentDao

    abstract fun mortalityDao(): MortalityDao

    abstract fun pendingSyncDao(): PendingSyncDao

    abstract fun calfRegistrationDao(): CalfRegistrationDao

    abstract fun supplierDao(): SupplierDao

    abstract fun locationFeedDao(): LocationFeedDao

    abstract fun animalCostDao(): AnimalCostDao
}