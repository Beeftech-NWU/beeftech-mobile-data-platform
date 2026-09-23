package com.beeftech.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Existing Base Entities
import com.beeftech.database.entity.Animal
import com.beeftech.database.entity.AnimalCost
import com.beeftech.database.entity.AnimalGroupMembershipEntity
import com.beeftech.database.entity.AnimalMovementEntity
import com.beeftech.database.entity.AnimalGroup
import com.beeftech.database.entity.FarmerAddressEntity
import com.beeftech.database.entity.FarmerEntity
import com.beeftech.database.entity.FarmerRoleEntity
import com.beeftech.database.entity.FeedCribEntity
import com.beeftech.database.entity.FeedCribReadingEntity
import com.beeftech.database.entity.FeedCribReadingValueEntity
import com.beeftech.database.entity.LocationEntity
import com.beeftech.database.entity.Mortality
import com.beeftech.database.entity.PenEntity
import com.beeftech.database.entity.PendingSync
import com.beeftech.database.entity.Role
import com.beeftech.database.entity.SyncBackupEntity
import com.beeftech.database.entity.SyncBatchEntity
import com.beeftech.database.entity.Treatment
import com.beeftech.database.entity.User

// Phase 3 Entities
import com.beeftech.database.entity.AnimalIdentifierEntity
import com.beeftech.database.entity.AnimalMediaEntity
import com.beeftech.database.entity.AnimalWeightEntity

// Phase 6 Entities
import com.beeftech.database.entity.AnimalOwnershipEntity
import com.beeftech.database.entity.AnimalPurchaseEntity

// Phase 7 Entity
import com.beeftech.database.entity.CalfRegistrationEntity

// DAOs
import com.beeftech.database.dao.AnimalDao
import com.beeftech.database.dao.AnimalCostDao
import com.beeftech.database.dao.AnimalGroupDao
import com.beeftech.database.dao.AnimalGroupMembershipDao
import com.beeftech.database.dao.AnimalMovementDao
import com.beeftech.database.dao.AnimalIdentifierDao
import com.beeftech.database.dao.AnimalMediaDao
import com.beeftech.database.dao.AnimalWeightDao
import com.beeftech.database.dao.AnimalOwnershipDao
import com.beeftech.database.dao.AnimalPurchaseDao
import com.beeftech.database.dao.CalfRegistrationDao
import com.beeftech.database.dao.FarmerDao
import com.beeftech.database.dao.FeedCribDao
import com.beeftech.database.dao.FeedCribReadingDao
import com.beeftech.database.dao.LocationDao
import com.beeftech.database.dao.MortalityDao
import com.beeftech.database.dao.PenDao
import com.beeftech.database.dao.PendingSyncDao
import com.beeftech.database.dao.RoleDao
import com.beeftech.database.dao.SyncBatchDao
import com.beeftech.database.dao.TreatmentDao
import com.beeftech.database.dao.UserDao

@Database(
    entities = [
        // Base / Existing Entities
        Animal::class,
        AnimalCost::class,
        AnimalGroupMembershipEntity::class,
        AnimalMovementEntity::class,
        AnimalGroup::class,
        FarmerEntity::class,
        FarmerAddressEntity::class,
        FarmerRoleEntity::class,
        LocationEntity::class,
        PenEntity::class,
        FeedCribEntity::class,
        FeedCribReadingEntity::class,
        FeedCribReadingValueEntity::class,
        Role::class,
        User::class,
        SyncBatchEntity::class,
        SyncBackupEntity::class,
        Treatment::class,
        Mortality::class,
        PendingSync::class,
        
        // Phase 3 Entities
        AnimalIdentifierEntity::class,
        AnimalMediaEntity::class,
        AnimalWeightEntity::class,
        
        // Phase 6 Entities
        AnimalOwnershipEntity::class,
        AnimalPurchaseEntity::class,
        
        // Phase 7 Entity
        CalfRegistrationEntity::class
    ],
    version = 12,
    exportSchema = true
)
abstract class BeefTechDatabase : RoomDatabase() {

    // =========================================================================
    // Abstract DAO Getters
    // =========================================================================
    
    abstract fun animalDao(): AnimalDao
    abstract fun animalCostDao(): AnimalCostDao
    abstract fun animalGroupMembershipDao(): AnimalGroupMembershipDao
    abstract fun animalMovementDao(): AnimalMovementDao
    abstract fun animalGroupDao(): AnimalGroupDao
    abstract fun farmerDao(): FarmerDao
    abstract fun locationDao(): LocationDao
    abstract fun penDao(): PenDao
    abstract fun feedCribDao(): FeedCribDao
    abstract fun feedCribReadingDao(): FeedCribReadingDao
    abstract fun roleDao(): RoleDao
    abstract fun userDao(): UserDao
    abstract fun syncBatchDao(): SyncBatchDao
    abstract fun treatmentDao(): TreatmentDao
    abstract fun mortalityDao(): MortalityDao
    abstract fun pendingSyncDao(): PendingSyncDao

    // Phase 3 DAOs
    abstract fun animalIdentifierDao(): AnimalIdentifierDao
    abstract fun animalMediaDao(): AnimalMediaDao
    abstract fun animalWeightDao(): AnimalWeightDao

    // Phase 6 DAOs
    abstract fun animalOwnershipDao(): AnimalOwnershipDao
    abstract fun animalPurchaseDao(): AnimalPurchaseDao

    // Phase 7 DAO
    abstract fun calfRegistrationDao(): CalfRegistrationDao


    // =========================================================================
    // Migration Configurations
    // =========================================================================
    companion object {

        /**
         * Phase 3 Migration (Version 9 -> 10):
         * - Creates `animal_identifiers`
         * - Creates `animal_media`
         * - Creates `animal_weights`
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create animal_identifiers
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `animal_identifiers` (
                        `identifier_id` TEXT NOT NULL,
                        `animal_id` TEXT NOT NULL,
                        `identifier_type` TEXT NOT NULL,
                        `identifier_value` TEXT NOT NULL,
                        `valid_from` TEXT,
                        `valid_to` TEXT,
                        PRIMARY KEY(`identifier_id`),
                        FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_identifiers_animal_id` ON `animal_identifiers` (`animal_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_identifiers_identifier_type_identifier_value` ON `animal_identifiers` (`identifier_type`, `identifier_value`)")

                // 2. Create animal_media
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `animal_media` (
                        `media_id` TEXT NOT NULL,
                        `animal_id` TEXT NOT NULL,
                        `file_path` TEXT NOT NULL,
                        `media_type` TEXT NOT NULL,
                        `created_at` TEXT NOT NULL,
                        PRIMARY KEY(`media_id`),
                        FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_media_animal_id` ON `animal_media` (`animal_id`)")

                // 3. Create animal_weights
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `animal_weights` (
                        `weight_id` TEXT NOT NULL,
                        `animal_id` TEXT NOT NULL,
                        `weight_kg` REAL NOT NULL,
                        `weigh_date` TEXT NOT NULL,
                        `notes` TEXT,
                        PRIMARY KEY(`weight_id`),
                        FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_weights_animal_id` ON `animal_weights` (`animal_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_weights_weigh_date` ON `animal_weights` (`weigh_date`)")
            }
        }

        /**
         * Phase 6 & Phase 7 Migration (Version 10 -> 11):
         * - Creates `animal_ownerships` and `animal_purchases`
         * - Drops obsolete `suppliers` and `location_feeds` tables
         * - Refactors `calf_registrations` to key explicitly on `registered_animal_id`
         * - Removes obsolete orphan tables
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // --- PHASE 6 ---
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `animal_ownerships` (
                        `ownership_id` TEXT NOT NULL,
                        `animal_id` TEXT NOT NULL,
                        `owner_name` TEXT NOT NULL,
                        `ownership_percentage` REAL NOT NULL,
                        `start_date` TEXT NOT NULL,
                        `end_date` TEXT,
                        PRIMARY KEY(`ownership_id`),
                        FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_ownerships_animal_id` ON `animal_ownerships` (`animal_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_ownerships_owner_name` ON `animal_ownerships` (`owner_name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_ownerships_start_date` ON `animal_ownerships` (`start_date`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `animal_purchases` (
                        `purchase_id` TEXT NOT NULL,
                        `animal_id` TEXT NOT NULL,
                        `purchase_price` REAL NOT NULL,
                        `purchase_date` TEXT NOT NULL,
                        `seller_name` TEXT NOT NULL,
                        `notes` TEXT,
                        PRIMARY KEY(`purchase_id`),
                        FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_purchases_animal_id` ON `animal_purchases` (`animal_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_purchases_purchase_date` ON `animal_purchases` (`purchase_date`)")

                // Ensure treatments table schema matches v11 Treatment entity
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `treatments_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `animalId` TEXT NOT NULL,
                        `disease` TEXT NOT NULL,
                        `treatmentName` TEXT NOT NULL,
                        `batchNumber` TEXT NOT NULL,
                        `volumeUsed` TEXT NOT NULL,
                        `cost` REAL NOT NULL,
                        `gpsLat` REAL NOT NULL DEFAULT 0.0,
                        `gpsLng` REAL NOT NULL DEFAULT 0.0,
                        `timestamp` INTEGER NOT NULL,
                        `deviceId` TEXT NOT NULL DEFAULT '',
                        `recordguid` TEXT NOT NULL DEFAULT '',
                        `syncStatus` TEXT NOT NULL DEFAULT 'PENDING',
                        `syncedAt` INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `treatments_new` (
                        `id`, `animalId`, `disease`, `treatmentName`, `batchNumber`, `volumeUsed`, `cost`, `timestamp`
                    )
                    SELECT `id`, `animalId`, `disease`, `treatmentName`, `batchNumber`, `volumeUsed`, `cost`, `timestamp`
                    FROM `treatments`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE IF EXISTS `treatments` ")
                db.execSQL("ALTER TABLE `treatments_new` RENAME TO `treatments` ")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_treatments_recordguid` ON `treatments` (`recordguid`)")

                // Ensure animal_movements table schema matches v11 AnimalMovementEntity
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `animal_movements_new` (
                        `movement_id` TEXT NOT NULL,
                        `animal_id` TEXT NOT NULL,
                        `source_farm_id` TEXT,
                        `source_pen_id` TEXT,
                        `destination_farm_id` TEXT NOT NULL,
                        `destination_pen_id` TEXT NOT NULL,
                        `movement_date` TEXT NOT NULL,
                        `feed_location_type` TEXT,
                        `notes` TEXT,
                        PRIMARY KEY(`movement_id`),
                        FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE IF EXISTS `animal_movements` ")
                db.execSQL("ALTER TABLE `animal_movements_new` RENAME TO `animal_movements` ")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_movements_animal_id` ON `animal_movements` (`animal_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_movements_destination_farm_id` ON `animal_movements` (`destination_farm_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_movements_destination_pen_id` ON `animal_movements` (`destination_pen_id`)")

                // Remove obsolete tables replaced by Phase 6 changes
                db.execSQL("DROP TABLE IF EXISTS `suppliers` ")
                db.execSQL("DROP TABLE IF EXISTS `location_feeds` ")

                // --- PHASE 7 ---
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `calf_registrations_new` (
                        `registration_id` TEXT NOT NULL,
                        `registered_animal_id` TEXT NOT NULL,
                        `dam_id` TEXT,
                        `sire_id` TEXT,
                        `birth_weight_kg` REAL,
                        `calving_ease` TEXT,
                        `registration_date` TEXT NOT NULL,
                        PRIMARY KEY(`registration_id`),
                        FOREIGN KEY(`registered_animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`dam_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`sire_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )

                // Replace old calf_registrations table with new schema
                db.execSQL("DROP TABLE IF EXISTS `calf_registrations` ")
                db.execSQL("ALTER TABLE `calf_registrations_new` RENAME TO `calf_registrations` ")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calf_registrations_registered_animal_id` ON `calf_registrations` (`registered_animal_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calf_registrations_dam_id` ON `calf_registrations` (`dam_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calf_registrations_sire_id` ON `calf_registrations` (`sire_id`)")

                // Remove orphan tables
                db.execSQL("DROP TABLE IF EXISTS `orphan_entity_one` ")
                db.execSQL("DROP TABLE IF EXISTS `orphan_entity_two` ")
                db.execSQL("DROP TABLE IF EXISTS `orphan_entity_three` ")
                db.execSQL("DROP TABLE IF EXISTS `orphan_entity_four` ")
            }
        }

        /**
         * Migration (Version 11 -> 12):
         * - Ensures all missing columns and unique indices match Room entity specs
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                fun addColumnIfNotExists(table: String, columnDef: String) {
                    try {
                        db.execSQL("ALTER TABLE `$table` ADD COLUMN $columnDef")
                    } catch (_: Exception) {
                        // Column already exists
                    }
                }

                // 1. animals
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animals_recordguid` ON `animals` (`recordguid`)")

                // 2. animal_costs
                addColumnIfNotExists("animal_costs", "`gpsLat` REAL NOT NULL DEFAULT 0.0")
                addColumnIfNotExists("animal_costs", "`gpsLng` REAL NOT NULL DEFAULT 0.0")
                addColumnIfNotExists("animal_costs", "`record_guid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_costs_record_guid` ON `animal_costs` (`record_guid`)")

                // 3. animal_group_memberships
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_group_memberships_record_guid` ON `animal_group_memberships` (`record_guid`)")

                // 4. farmers
                addColumnIfNotExists("farmers", "`record_guid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_farmers_record_guid` ON `farmers` (`record_guid`)")

                // 5. mortalities
                addColumnIfNotExists("mortalities", "`record_guid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_mortalities_record_guid` ON `mortalities` (`record_guid`)")

                // 6. treatments
                addColumnIfNotExists("treatments", "`gpsLat` REAL NOT NULL DEFAULT 0.0")
                addColumnIfNotExists("treatments", "`gpsLng` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_treatments_recordguid` ON `treatments` (`recordguid`)")
            }
        }
    }
}
