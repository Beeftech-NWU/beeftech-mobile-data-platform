package com.beeftech.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.beeftech.database.entity.LocationEntity
import com.beeftech.database.entity.FarmerRoleEntity
import com.beeftech.database.entity.FarmerAddressEntity
import com.beeftech.database.entity.FarmerEntity
import com.beeftech.database.dao.LocationDao
import com.beeftech.database.dao.FarmerDao
import com.beeftech.database.dao.AnimalCostDao
import com.beeftech.database.dao.AnimalMovementDao
import com.beeftech.database.dao.CalfRegistrationDao
import com.beeftech.database.dao.LocationFeedDao
import com.beeftech.database.dao.MortalityDao
import com.beeftech.database.dao.PendingSyncDao
import com.beeftech.database.dao.SupplierDao
import com.beeftech.database.dao.TreatmentDao
import com.beeftech.database.dao.RoleDao
import com.beeftech.database.dao.UserDao
import com.beeftech.database.dao.AnimalGroupDao
import com.beeftech.database.dao.AnimalDao
import com.beeftech.database.entity.AnimalCost
import com.beeftech.database.entity.AnimalMovement
import com.beeftech.database.entity.CalfRegistration
import com.beeftech.database.entity.LocationFeed
import com.beeftech.database.entity.Mortality
import com.beeftech.database.entity.PendingSync
import com.beeftech.database.entity.Supplier
import com.beeftech.database.entity.Treatment
import com.beeftech.database.entity.Role
import com.beeftech.database.entity.User
import com.beeftech.database.entity.AnimalGroup
import com.beeftech.database.entity.Animal
import com.beeftech.database.entity.PenEntity
import com.beeftech.database.entity.FeedCribEntity
import com.beeftech.database.entity.FeedCribReadingEntity
import com.beeftech.database.entity.FeedCribReadingValueEntity
import com.beeftech.database.entity.SyncBatchEntity
import com.beeftech.database.entity.SyncBackupEntity
import com.beeftech.database.dao.PenDao
import com.beeftech.database.dao.FeedCribDao
import com.beeftech.database.dao.FeedCribReadingDao
import com.beeftech.database.dao.SyncBatchDao
import com.beeftech.database.entity.AnimalWeightEntity
import com.beeftech.database.entity.AnimalGroupMembershipEntity
import com.beeftech.database.entity.*
import com.beeftech.database.dao.AnimalWeightDao
import com.beeftech.database.dao.AnimalGroupMembershipDao
import com.beeftech.database.dao.*

@Database(
    entities = [
        AnimalMovement::class,
        Treatment::class,
        Mortality::class,
        PendingSync::class,
        CalfRegistration::class,
        Supplier::class,
        LocationFeed::class,
        AnimalCost::class,
        Role::class,
        User::class,
        FarmerEntity::class,
        FarmerAddressEntity::class,
        FarmerRoleEntity::class,
        LocationEntity::class,
        Animal::class,
        AnimalGroup::class,
        PenEntity::class,
        FeedCribEntity::class,
        FeedCribReadingEntity::class,
        FeedCribReadingValueEntity::class,
        SyncBatchEntity::class,
        SyncBackupEntity::class,
        AnimalWeightEntity::class,
        AnimalGroupMembershipEntity::class

    ],
    version = 8,
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

    abstract fun roleDao(): RoleDao
    abstract fun userDao(): UserDao
    abstract fun farmerDao(): FarmerDao
    abstract fun locationDao(): LocationDao
    abstract fun animalDao(): AnimalDao
    abstract fun animalGroupDao(): AnimalGroupDao

    abstract fun penDao(): PenDao

    abstract fun feedCribDao(): FeedCribDao

    abstract fun feedCribReadingDao(): FeedCribReadingDao

    abstract fun syncBatchDao(): SyncBatchDao
    abstract fun animalWeightDao(): AnimalWeightDao
    abstract fun animalGroupMembershipDao(): AnimalGroupMembershipDao
}
