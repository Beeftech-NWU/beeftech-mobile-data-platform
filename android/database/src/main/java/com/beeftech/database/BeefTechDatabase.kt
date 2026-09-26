package com.beeftech.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

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

// Phase 5 Entities
import com.beeftech.database.entity.CostType

// Phase 6 Entities
import com.beeftech.database.entity.AnimalOwnershipEntity
import com.beeftech.database.entity.AnimalPurchaseEntity

// Phase 7 Entity
import com.beeftech.database.entity.CalfRegistrationEntity

// DAOs
import com.beeftech.database.dao.AnimalDao
import com.beeftech.database.dao.AnimalCostDao
import com.beeftech.database.dao.CostTypeDao
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
        CalfRegistrationEntity::class,

        // Phase 5 Entity
        CostType::class
    ],
    version = 14,
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

    // Phase 5 DAO
    abstract fun costTypeDao(): CostTypeDao


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
         * - Ensures all 26 tables, columns, and indices match Room entity specs
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
                db.execSQL("CREATE TABLE IF NOT EXISTS `animals` (`animalId` TEXT NOT NULL, `tagNumber` TEXT, `oldTagNumber` TEXT, `temperatureNumber` TEXT, `referenceNumber` TEXT, `massKg` REAL, `birthdate` INTEGER NOT NULL, `breed` TEXT NOT NULL, `gender` TEXT, `age` INTEGER, `condition` TEXT, `hideColour` TEXT, `brandMark` TEXT, `parentId` TEXT, `animalGroupId` TEXT, `photoPath` TEXT, `videoPath` TEXT, `gpsLat` REAL NOT NULL, `gpsLng` REAL NOT NULL, `captureAt` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `recordguid` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, `syncedat` INTEGER, PRIMARY KEY(`animalId`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_tagNumber` ON `animals` (`tagNumber`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_temperatureNumber` ON `animals` (`temperatureNumber`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_parentId` ON `animals` (`parentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_animalGroupId` ON `animals` (`animalGroupId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animals_recordguid` ON `animals` (`recordguid`)")

                // 2. animal_costs
                db.execSQL("CREATE TABLE IF NOT EXISTS `animal_costs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `animalId` TEXT NOT NULL, `costType` TEXT NOT NULL, `amount` REAL NOT NULL, `description` TEXT NOT NULL DEFAULT '', `gpsLat` REAL NOT NULL, `gpsLng` REAL NOT NULL, `timestamp` INTEGER NOT NULL, `record_guid` TEXT NOT NULL)")
                addColumnIfNotExists("animal_costs", "`gpsLat` REAL NOT NULL DEFAULT 0.0")
                addColumnIfNotExists("animal_costs", "`gpsLng` REAL NOT NULL DEFAULT 0.0")
                addColumnIfNotExists("animal_costs", "`record_guid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_costs_record_guid` ON `animal_costs` (`record_guid`)")

                // 3. animal_group_memberships
                db.execSQL("CREATE TABLE IF NOT EXISTS `animal_group_memberships` (`membership_id` TEXT NOT NULL, `animal_id` TEXT NOT NULL, `group_id` TEXT NOT NULL, `joined_at` INTEGER NOT NULL, `left_at` INTEGER, `record_guid` TEXT NOT NULL, PRIMARY KEY(`membership_id`))")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_group_memberships_record_guid` ON `animal_group_memberships` (`record_guid`)")

                // 4. animal_movements
                db.execSQL("CREATE TABLE IF NOT EXISTS `animal_movements` (`movement_id` TEXT NOT NULL, `animal_id` TEXT NOT NULL, `source_farm_id` TEXT, `source_pen_id` TEXT, `destination_farm_id` TEXT NOT NULL, `destination_pen_id` TEXT NOT NULL, `movement_date` TEXT NOT NULL, `feed_location_type` TEXT, `notes` TEXT, PRIMARY KEY(`movement_id`), FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_movements_animal_id` ON `animal_movements` (`animal_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_movements_destination_farm_id` ON `animal_movements` (`destination_farm_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_movements_destination_pen_id` ON `animal_movements` (`destination_pen_id`)")

                // 5. animal_groups
                db.execSQL("CREATE TABLE IF NOT EXISTS `animal_groups` (`animalGroupId` TEXT NOT NULL, `groupName` TEXT NOT NULL, `description` TEXT, PRIMARY KEY(`animalGroupId`))")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_groups_groupName` ON `animal_groups` (`groupName`)")

                // 6. farmers
                db.execSQL("CREATE TABLE IF NOT EXISTS `farmers` (`farmer_id` TEXT NOT NULL, `client_code` TEXT, `organisation_name` TEXT, `vat_number` TEXT, `email_address` TEXT, `gps_latitude` REAL, `gps_longitude` REAL, `sync_status` TEXT NOT NULL, `record_guid` TEXT NOT NULL, PRIMARY KEY(`farmer_id`))")
                addColumnIfNotExists("farmers", "`record_guid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_farmers_record_guid` ON `farmers` (`record_guid`)")

                // 7. farmer_addresses
                db.execSQL("CREATE TABLE IF NOT EXISTS `farmer_addresses` (`address_id` TEXT NOT NULL, `farmer_id` TEXT NOT NULL, `address_type` TEXT, `address_line_1` TEXT, `province` TEXT, `postal_code` TEXT, `gps_latitude` REAL, `gps_longitude` REAL, PRIMARY KEY(`address_id`))")

                // 8. farmer_roles
                db.execSQL("CREATE TABLE IF NOT EXISTS `farmer_roles` (`farmer_role_id` TEXT NOT NULL, `farmer_id` TEXT NOT NULL, `role_id` TEXT NOT NULL, PRIMARY KEY(`farmer_role_id`))")

                // 9. locations
                db.execSQL("CREATE TABLE IF NOT EXISTS `locations` (`location_id` TEXT NOT NULL, `location_code` TEXT, `location_name` TEXT, `location_type` TEXT, PRIMARY KEY(`location_id`))")

                // 10. pens
                db.execSQL("CREATE TABLE IF NOT EXISTS `pens` (`id` TEXT NOT NULL, `name` TEXT, PRIMARY KEY(`id`))")

                // 11. feed_cribs
                db.execSQL("CREATE TABLE IF NOT EXISTS `feed_cribs` (`id` TEXT NOT NULL, `name` TEXT, PRIMARY KEY(`id`))")

                // 12. feed_crib_readings
                db.execSQL("CREATE TABLE IF NOT EXISTS `feed_crib_readings` (`id` TEXT NOT NULL, `cribId` TEXT NOT NULL, PRIMARY KEY(`id`))")

                // 13. feed_crib_reading_values
                db.execSQL("CREATE TABLE IF NOT EXISTS `feed_crib_reading_values` (`id` TEXT NOT NULL, `readingId` TEXT NOT NULL, `value` REAL NOT NULL, PRIMARY KEY(`id`))")

                // 14. roles
                db.execSQL("CREATE TABLE IF NOT EXISTS `roles` (`role_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `role_name` TEXT NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_roles_role_name` ON `roles` (`role_name`)")

                // 15. users
                db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`user_id` TEXT NOT NULL, `username` TEXT NOT NULL, `pin_hash` TEXT, `failed_pin_attempts` INTEGER NOT NULL, `role` INTEGER, `device_assigned_id` TEXT, `device_last_sync` INTEGER, `failed_sync_attempts` INTEGER NOT NULL, PRIMARY KEY(`user_id`), FOREIGN KEY(`role`) REFERENCES `roles`(`role_id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_users_role` ON `users` (`role`)")

                // 16. sync_batches
                db.execSQL("CREATE TABLE IF NOT EXISTS `sync_batches` (`id` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")

                // 17. sync_backups
                db.execSQL("CREATE TABLE IF NOT EXISTS `sync_backups` (`id` TEXT NOT NULL, `batchId` TEXT NOT NULL, `sync_status` TEXT NOT NULL, PRIMARY KEY(`id`))")
                addColumnIfNotExists("sync_backups", "`sync_status` TEXT NOT NULL DEFAULT 'PENDING'")

                // 18. treatments
                db.execSQL("CREATE TABLE IF NOT EXISTS `treatments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `animalId` TEXT NOT NULL, `disease` TEXT NOT NULL, `treatmentName` TEXT NOT NULL, `batchNumber` TEXT NOT NULL, `volumeUsed` TEXT NOT NULL, `cost` REAL NOT NULL, `gpsLat` REAL NOT NULL, `gpsLng` REAL NOT NULL, `timestamp` INTEGER NOT NULL, `deviceId` TEXT NOT NULL DEFAULT '', `recordguid` TEXT NOT NULL DEFAULT '', `syncStatus` TEXT NOT NULL DEFAULT 'PENDING', `syncedAt` INTEGER)")
                addColumnIfNotExists("treatments", "`gpsLat` REAL NOT NULL DEFAULT 0.0")
                addColumnIfNotExists("treatments", "`gpsLng` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_treatments_recordguid` ON `treatments` (`recordguid`)")

                // 19. mortalities
                db.execSQL("CREATE TABLE IF NOT EXISTS `mortalities` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `animalId` TEXT NOT NULL, `causeOfDeath` TEXT NOT NULL, `responsibleWorker` TEXT NOT NULL DEFAULT '', `notes` TEXT, `timestamp` INTEGER NOT NULL, `record_guid` TEXT NOT NULL)")
                addColumnIfNotExists("mortalities", "`record_guid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_mortalities_record_guid` ON `mortalities` (`record_guid`)")

                // 20. pending_sync
                db.execSQL("CREATE TABLE IF NOT EXISTS `pending_sync` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `entityType` TEXT NOT NULL, `entityId` TEXT NOT NULL, `operation` TEXT NOT NULL, `payload` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `retryCount` INTEGER NOT NULL)")

                // 21. animal_identifiers
                db.execSQL("CREATE TABLE IF NOT EXISTS `animal_identifiers` (`identifier_id` TEXT NOT NULL, `animal_id` TEXT NOT NULL, `identifier_type` TEXT NOT NULL, `identifier_value` TEXT NOT NULL, `valid_from` TEXT, `valid_to` TEXT, PRIMARY KEY(`identifier_id`), FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_identifiers_animal_id` ON `animal_identifiers` (`animal_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_identifiers_identifier_type_identifier_value` ON `animal_identifiers` (`identifier_type`, `identifier_value`)")

                // 22. animal_media
                db.execSQL("CREATE TABLE IF NOT EXISTS `animal_media` (`media_id` TEXT NOT NULL, `animal_id` TEXT NOT NULL, `file_path` TEXT NOT NULL, `media_type` TEXT NOT NULL, `created_at` TEXT NOT NULL, PRIMARY KEY(`media_id`), FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_media_animal_id` ON `animal_media` (`animal_id`)")

                // 23. animal_weights
                db.execSQL("CREATE TABLE IF NOT EXISTS `animal_weights` (`weight_id` TEXT NOT NULL, `animal_id` TEXT NOT NULL, `weight_kg` REAL NOT NULL, `weigh_date` TEXT NOT NULL, `notes` TEXT, PRIMARY KEY(`weight_id`), FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_weights_animal_id` ON `animal_weights` (`animal_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_weights_weigh_date` ON `animal_weights` (`weigh_date`)")

                // 24. animal_ownerships
                db.execSQL("CREATE TABLE IF NOT EXISTS `animal_ownerships` (`ownership_id` TEXT NOT NULL, `animal_id` TEXT NOT NULL, `owner_name` TEXT NOT NULL, `ownership_percentage` REAL NOT NULL, `start_date` TEXT NOT NULL, `end_date` TEXT, PRIMARY KEY(`ownership_id`), FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_ownerships_animal_id` ON `animal_ownerships` (`animal_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_ownerships_owner_name` ON `animal_ownerships` (`owner_name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_ownerships_start_date` ON `animal_ownerships` (`start_date`)")

                // 25. animal_purchases
                db.execSQL("CREATE TABLE IF NOT EXISTS `animal_purchases` (`purchase_id` TEXT NOT NULL, `animal_id` TEXT NOT NULL, `purchase_price` REAL NOT NULL, `purchase_date` TEXT NOT NULL, `seller_name` TEXT NOT NULL, `notes` TEXT, PRIMARY KEY(`purchase_id`), FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_purchases_animal_id` ON `animal_purchases` (`animal_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_purchases_purchase_date` ON `animal_purchases` (`purchase_date`)")

                // 26. calf_registrations
                db.execSQL("CREATE TABLE IF NOT EXISTS `calf_registrations` (`registration_id` TEXT NOT NULL, `registered_animal_id` TEXT NOT NULL, `dam_id` TEXT, `sire_id` TEXT, `birth_weight_kg` REAL, `calving_ease` TEXT, `registration_date` TEXT NOT NULL, PRIMARY KEY(`registration_id`), FOREIGN KEY(`registered_animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`dam_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`sire_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE SET NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calf_registrations_registered_animal_id` ON `calf_registrations` (`registered_animal_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calf_registrations_dam_id` ON `calf_registrations` (`dam_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calf_registrations_sire_id` ON `calf_registrations` (`sire_id`)")
            }
        }

        /**
         * Migration (Version 12 -> 13):
         * - Ensures all 26 tables, columns, and indices match Room entity specs
         *   for databases whose user_version was set to 12 prior to schema fix.
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_11_12.migrate(db)
            }
        }

        /**
         * Phase 5 Migration (Version 13 -> 14): Rebuild animal_costs
         * - Creates and seeds `cost_types`
         * - Rebuilds `animal_costs` with FK costType -> cost_types(code),
         *   source_entity / source_record_id, and unique record_guid
         * - Backfills one TREATMENT cost row per treatment with cost > 0
         */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {

                // 1. Lookup + seed
                db.execSQL("CREATE TABLE IF NOT EXISTS `cost_types` (`code` TEXT NOT NULL, `display_name` TEXT NOT NULL, `sort_order` INTEGER NOT NULL DEFAULT 0, `is_active` INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(`code`))")
                CostTypeSeed.execute(db)

                // 2. Keep every existing costType valid under the new FK.
                //    Unexpected values surface here as a data-quality list.
                //    They are inactive: valid for existing rows, but not
                //    offered for new costs until someone reconciles them.
                db.execSQL("INSERT OR IGNORE INTO `cost_types` (`code`, `display_name`, `sort_order`, `is_active`) SELECT DISTINCT `costType`, `costType`, 1000, 0 FROM `animal_costs`")

                // 3. Rebuild animal_costs
                db.execSQL("CREATE TABLE IF NOT EXISTS `animal_costs_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `animalId` TEXT NOT NULL, `costType` TEXT NOT NULL, `amount` REAL NOT NULL, `description` TEXT NOT NULL DEFAULT '', `gpsLat` REAL NOT NULL, `gpsLng` REAL NOT NULL, `timestamp` INTEGER NOT NULL, `source_entity` TEXT, `source_record_id` TEXT, `record_guid` TEXT NOT NULL, FOREIGN KEY(`costType`) REFERENCES `cost_types`(`code`) ON UPDATE CASCADE ON DELETE RESTRICT )")
                db.execSQL("INSERT INTO `animal_costs_new` (`id`, `animalId`, `costType`, `amount`, `description`, `gpsLat`, `gpsLng`, `timestamp`, `source_entity`, `source_record_id`, `record_guid`) SELECT `id`, `animalId`, `costType`, `amount`, `description`, `gpsLat`, `gpsLng`, `timestamp`, NULL, NULL, `record_guid` FROM `animal_costs`")
                db.execSQL("DROP TABLE `animal_costs`")
                db.execSQL("ALTER TABLE `animal_costs_new` RENAME TO `animal_costs`")

                // 4. Give any blank GUID (left by MIGRATION_11_12's DEFAULT '') a real one
                val blankIds = mutableListOf<Long>()
                db.query("SELECT `id` FROM `animal_costs` WHERE `record_guid` = ''").use { c ->
                    while (c.moveToNext()) blankIds += c.getLong(0)
                }
                blankIds.forEach { id ->
                    db.execSQL("UPDATE `animal_costs` SET `record_guid` = ? WHERE `id` = ?", arrayOf<Any>(UUID.randomUUID().toString(), id))
                }

                // 5. Indexes (names must match Room's generated names)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_costs_record_guid` ON `animal_costs` (`record_guid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_costs_costType` ON `animal_costs` (`costType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_costs_animalId_costType_timestamp` ON `animal_costs` (`animalId`, `costType`, `timestamp`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_costs_source_entity_source_record_id` ON `animal_costs` (`source_entity`, `source_record_id`)")

                // 6. Backfill derived treatment costs
                val treatmentRows = mutableListOf<Array<Any>>()
                db.query("SELECT `animalId`, `cost`, `treatmentName`, `gpsLat`, `gpsLng`, `timestamp`, `recordguid` FROM `treatments` WHERE `cost` > 0").use { c ->
                    while (c.moveToNext()) {
                        treatmentRows.add(
                            arrayOf(
                                c.getString(0), c.getDouble(1), c.getString(2),
                                c.getDouble(3), c.getDouble(4), c.getLong(5),
                                c.getString(6), UUID.randomUUID().toString()
                            )
                        )
                    }
                }
                treatmentRows.forEach { args ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO `animal_costs` (`animalId`, `costType`, `amount`, `description`, `gpsLat`, `gpsLng`, `timestamp`, `source_entity`, `source_record_id`, `record_guid`) VALUES (?, 'TREATMENT', ?, ?, ?, ?, ?, 'TREATMENT', ?, ?)",
                        args
                    )
                }
            }
        }

        /*
         * Seeds lookup tables.
         *
         * onCreate covers a fresh install, where migrations do not run.
         * onOpen also covers a destructive migration (e.g. a downgrade),
         * which recreates the tables without calling onCreate. The seed
         * is INSERT OR IGNORE, so running it on every open is safe.
         */
        val SEED_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                CostTypeSeed.execute(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                CostTypeSeed.execute(db)
            }
        }
    }
}
