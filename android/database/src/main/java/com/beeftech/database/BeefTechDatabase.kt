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
    version = 19,
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
         * - Keeps every original `animal_movements`, `calf_registrations` and
         *   `suppliers` row (D2, R0.2): nothing is dropped without a home.
         *   `legacy_*` copies of all three are kept permanently until R3/R6
         *   move their data into a real schema.
         * - Refactors `calf_registrations` to key explicitly on `registered_animal_id`
         * - Fixes the treatments rebuild so a blank `recordguid` can't crash the
         *   upgrade when `CREATE UNIQUE INDEX` runs (D3, R0.3)
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {

                // --- R0.2: preserve every original row before any DROP below. ---
                // These are the only surviving copy for a row whose old animalId
                // (a tag, or a v10 UUID) can't be resolved to an `animals` row,
                // and for `suppliers`, which has no home in the schema yet.
                db.execSQL("CREATE TABLE IF NOT EXISTS `legacy_animal_movements` AS SELECT * FROM `animal_movements`")
                db.execSQL("CREATE TABLE IF NOT EXISTS `legacy_calf_registrations` AS SELECT * FROM `calf_registrations`")
                db.execSQL("CREATE TABLE IF NOT EXISTS `legacy_suppliers` AS SELECT * FROM `suppliers`")

                // Resolves a v10 `animalId` (which may already be a UUID, or may be
                // a tag written by a build before D1) to a real `animals.animalId`.
                fun resolveAnimalId(oldAnimalId: String?): String? {
                    if (oldAnimalId.isNullOrBlank()) return null
                    db.query("SELECT `animalId` FROM `animals` WHERE `animalId` = ?", arrayOf<Any>(oldAnimalId)).use {
                        if (it.moveToFirst()) return it.getString(0)
                    }
                    db.query("SELECT `animalId` FROM `animals` WHERE `tagNumber` = ?", arrayOf<Any>(oldAnimalId)).use {
                        if (it.moveToFirst()) return it.getString(0)
                    }
                    db.query(
                        "SELECT `animal_id` FROM `animal_identifiers` WHERE `identifier_type` = 'TAG' AND `identifier_value` = ? LIMIT 1",
                        arrayOf<Any>(oldAnimalId)
                    ).use {
                        if (it.moveToFirst()) return it.getString(0)
                    }
                    return null
                }

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

                // Ensure treatments table schema matches v11 Treatment entity.
                // v10's `treatments` table never had gpsLat/gpsLng/deviceId/
                // recordguid/syncStatus/syncedAt -- they are new columns here,
                // filled from DEFAULT for every existing row. That means every
                // migrated row gets recordguid = '', so the CREATE UNIQUE INDEX
                // below throws with 2+ rows unless it's backfilled first (D3).
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

                // R0.3 (D3): give every blank recordguid a real UUID before the
                // UNIQUE index is created.
                run {
                    val blankTreatmentIds = mutableListOf<Long>()
                    db.query("SELECT `id` FROM `treatments` WHERE `recordguid` IS NULL OR `recordguid` = ''").use { c ->
                        while (c.moveToNext()) blankTreatmentIds += c.getLong(0)
                    }
                    blankTreatmentIds.forEach { id ->
                        db.execSQL(
                            "UPDATE `treatments` SET `recordguid` = ? WHERE `id` = ?",
                            arrayOf<Any>(UUID.randomUUID().toString(), id)
                        )
                    }
                }
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

                // R0.2 (D2): recover every movement whose animal resolves, instead
                // of leaving `animal_movements` empty. This matches how the app
                // fills these columns today; R6 fixes the columns properly.
                db.query(
                    "SELECT `animalId`, `movementType`, `responsibleWorker`, `timestamp`, `recordguid` FROM `legacy_animal_movements`"
                ).use { c ->
                    while (c.moveToNext()) {
                        val resolvedAnimalId = resolveAnimalId(c.getString(0))
                        if (resolvedAnimalId != null) {
                            val oldRecordGuid = if (c.isNull(4)) null else c.getString(4)
                            val movementId = if (oldRecordGuid.isNullOrBlank()) UUID.randomUUID().toString() else oldRecordGuid
                            db.execSQL(
                                """
                                INSERT INTO `animal_movements` (
                                    `movement_id`, `animal_id`, `source_farm_id`, `source_pen_id`,
                                    `destination_farm_id`, `destination_pen_id`, `movement_date`,
                                    `feed_location_type`, `notes`
                                ) VALUES (?, ?, NULL, NULL, ?, '', ?, NULL, ?)
                                """.trimIndent(),
                                arrayOf<Any>(
                                    movementId, resolvedAnimalId, c.getString(1),
                                    c.getLong(3).toString(), c.getString(2)
                                )
                            )
                        }
                        // Rows whose animal doesn't resolve stay only in legacy_animal_movements.
                    }
                }

                // suppliers has no home in the schema yet (legacy_suppliers keeps the
                // data until R6 creates a real `suppliers` table -- it needs a
                // purchase_price that suppliers never recorded, and inventing one
                // would break rule 1).
                db.execSQL("DROP TABLE IF EXISTS `suppliers` ")

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

                // R0.2 (D2): recover every calf registration, instead of leaving
                // `calf_registrations` empty. For each old row, reuse the animal
                // if its tag resolves; otherwise create the animals/identifier/
                // media rows the current registration flow would have written.
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                db.query(
                    """
                    SELECT `animalId`, `birthdate`, `breed`, `damId`, `sireId`, `photoPath`,
                           `videoPath`, `gpsLat`, `gpsLng`, `captureAt`, `deviceId`, `recordguid`
                    FROM `legacy_calf_registrations`
                    """.trimIndent()
                ).use { c ->
                    while (c.moveToNext()) {
                        val oldTag = c.getString(0)
                        val birthdate = c.getLong(1)
                        val breed = c.getString(2)
                        val damTag = if (c.isNull(3)) null else c.getString(3)
                        val sireTag = if (c.isNull(4)) null else c.getString(4)
                        val photoPath = if (c.isNull(5)) null else c.getString(5)
                        val videoPath = if (c.isNull(6)) null else c.getString(6)
                        val gpsLat = c.getDouble(7)
                        val gpsLng = c.getDouble(8)
                        val captureAt = c.getLong(9)
                        val deviceId = c.getString(10)
                        val oldRecordGuid = if (c.isNull(11)) null else c.getString(11)

                        var resolvedAnimalId = resolveAnimalId(oldTag)
                        if (resolvedAnimalId == null) {
                            val newAnimalId = UUID.randomUUID().toString()
                            db.execSQL(
                                """
                                INSERT INTO `animals` (
                                    `animalId`, `tagNumber`, `oldTagNumber`, `temperatureNumber`, `referenceNumber`,
                                    `massKg`, `birthdate`, `breed`, `gender`, `age`, `condition`, `hideColour`,
                                    `brandMark`, `parentId`, `animalGroupId`, `photoPath`, `videoPath`,
                                    `gpsLat`, `gpsLng`, `captureAt`, `deviceId`, `recordguid`, `syncStatus`, `syncedat`
                                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                                """.trimIndent(),
                                arrayOf<Any?>(
                                    newAnimalId, oldTag, null, null, null,
                                    null, birthdate, breed, null, null, null, null,
                                    null, null, null, photoPath, videoPath,
                                    gpsLat, gpsLng, captureAt, deviceId,
                                    UUID.randomUUID().toString(), "PENDING", null
                                )
                            )
                            db.execSQL(
                                "INSERT INTO `animal_identifiers` (`identifier_id`, `animal_id`, `identifier_type`, `identifier_value`, `valid_from`, `valid_to`) VALUES (?, ?, 'TAG', ?, NULL, NULL)",
                                arrayOf<Any>(UUID.randomUUID().toString(), newAnimalId, oldTag)
                            )
                            if (!photoPath.isNullOrBlank()) {
                                db.execSQL(
                                    "INSERT INTO `animal_media` (`media_id`, `animal_id`, `file_path`, `media_type`, `created_at`) VALUES (?, ?, ?, 'PHOTO', ?)",
                                    arrayOf<Any>(UUID.randomUUID().toString(), newAnimalId, photoPath, captureAt.toString())
                                )
                            }
                            if (!videoPath.isNullOrBlank()) {
                                db.execSQL(
                                    "INSERT INTO `animal_media` (`media_id`, `animal_id`, `file_path`, `media_type`, `created_at`) VALUES (?, ?, ?, 'VIDEO', ?)",
                                    arrayOf<Any>(UUID.randomUUID().toString(), newAnimalId, videoPath, captureAt.toString())
                                )
                            }
                            resolvedAnimalId = newAnimalId
                        }

                        val damAnimalId = resolveAnimalId(damTag)
                        val sireAnimalId = resolveAnimalId(sireTag)
                        val registrationId = if (oldRecordGuid.isNullOrBlank()) UUID.randomUUID().toString() else oldRecordGuid
                        val registrationDate = dateFormat.format(java.util.Date(captureAt))

                        db.execSQL(
                            """
                            INSERT INTO `calf_registrations` (
                                `registration_id`, `registered_animal_id`, `dam_id`, `sire_id`,
                                `birth_weight_kg`, `calving_ease`, `registration_date`
                            ) VALUES (?, ?, ?, ?, NULL, NULL, ?)
                            """.trimIndent(),
                            arrayOf<Any?>(registrationId, resolvedAnimalId, damAnimalId, sireAnimalId, registrationDate)
                        )
                    }
                }

                // These four tables were never created by any migration; the
                // drops were a no-op, as is dropping `location_feeds` (the real
                // table is `location_feed` -- R6 migrates it, so it's left alone).
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

        /**
         * Phase 2 Migration (Version 14 -> 16): Referential Integrity Constraints
         *
         * Bumped to 16 (not 15) because branch fix/D1-calf-registration-animal-record
         * independently claimed version 15 with an unrelated schema (record_guid/sync
         * state on calf_registrations). Whichever of the two branches merges second
         * would otherwise collide with a same-version/different-schema database on
         * devices that installed a build from the other branch first.
         *
         * - Adds foreign keys to treatments, mortalities, animal_group_memberships, animals,
         *   farmer_addresses, farmer_roles, feed_crib_readings, feed_crib_reading_values, sync_backups
         * - Retypes farmer_roles.role_id to INTEGER (Long)
         * - Cleans up orphan records before applying constraints
         * - Verifies foreign keys at completion via PRAGMA foreign_key_check
         */
        val MIGRATION_14_16 = object : Migration(14, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // R0.4 (N1): seed `roles` before anything below reads it.
                // Nothing wrote to `roles` before #47's RoleSeed, and
                // RoleSeed.execute normally only runs in SEED_CALLBACK.onOpen,
                // which is after every migration finishes -- so `roles` was
                // empty while this migration ran, and the farmer_roles cleanup
                // below deleted every farmer role. INSERT OR IGNORE makes this
                // safe to run whether or not onOpen has seeded it already.
                RoleSeed.execute(db)

                // 1. Clean up orphan rows prior to constraint enforcement.
                // R0.5 (N2): a row keyed on a tag by a build before D1 gets one
                // chance to be re-keyed to the animal's UUID before it's
                // treated as an orphan (rule 3). Whatever is still deleted
                // below is copied into quarantine_<table> first, never
                // silently dropped.

                fun quarantineTable(table: String) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `quarantine_$table` AS SELECT * FROM `$table` WHERE 1 = 0")
                }

                fun quarantineAndDelete(table: String, whereClause: String) {
                    db.execSQL("INSERT INTO `quarantine_$table` SELECT * FROM `$table` WHERE $whereClause")
                    db.execSQL("DELETE FROM `$table` WHERE $whereClause")
                }

                fun rekeyTagToAnimalId(table: String, column: String) {
                    db.execSQL(
                        """
                        UPDATE `$table` SET `$column` = (
                            SELECT a.`animalId` FROM `animals` a WHERE a.`tagNumber` = `$table`.`$column` LIMIT 1
                        )
                        WHERE `$column` NOT IN (SELECT `animalId` FROM `animals`)
                        AND EXISTS (SELECT 1 FROM `animals` a WHERE a.`tagNumber` = `$table`.`$column`)
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        UPDATE `$table` SET `$column` = (
                            SELECT ai.`animal_id` FROM `animal_identifiers` ai
                            WHERE ai.`identifier_type` = 'TAG' AND ai.`identifier_value` = `$table`.`$column`
                            LIMIT 1
                        )
                        WHERE `$column` NOT IN (SELECT `animalId` FROM `animals`)
                        AND EXISTS (
                            SELECT 1 FROM `animal_identifiers` ai
                            WHERE ai.`identifier_type` = 'TAG' AND ai.`identifier_value` = `$table`.`$column`
                        )
                        """.trimIndent()
                    )
                }

                // CASCADE Orphans
                rekeyTagToAnimalId("treatments", "animalId")
                quarantineTable("treatments")
                quarantineAndDelete("treatments", "`animalId` NOT IN (SELECT `animalId` FROM `animals`)")

                rekeyTagToAnimalId("mortalities", "animalId")
                quarantineTable("mortalities")
                quarantineAndDelete("mortalities", "`animalId` NOT IN (SELECT `animalId` FROM `animals`)")
                // Keep only one mortality record per animalId if duplicates exist before enforcing UNIQUE(animalId)
                quarantineAndDelete("mortalities", "`id` NOT IN (SELECT MIN(`id`) FROM `mortalities` GROUP BY `animalId`)")

                rekeyTagToAnimalId("animal_group_memberships", "animal_id")
                quarantineTable("animal_group_memberships")
                quarantineAndDelete("animal_group_memberships", "`animal_id` NOT IN (SELECT `animalId` FROM `animals`)")
                // RESTRICT Orphans: memberships referencing non-existent animal_groups
                quarantineAndDelete("animal_group_memberships", "`group_id` NOT IN (SELECT `animalGroupId` FROM `animal_groups`)")

                // SET_NULL Orphans -- not a delete, nothing to quarantine
                db.execSQL("UPDATE `animals` SET `animalGroupId` = NULL WHERE `animalGroupId` IS NOT NULL AND `animalGroupId` NOT IN (SELECT `animalGroupId` FROM `animal_groups`)")

                quarantineTable("farmer_addresses")
                quarantineAndDelete("farmer_addresses", "`farmer_id` NOT IN (SELECT `farmer_id` FROM `farmers`)")

                quarantineTable("farmer_roles")
                quarantineAndDelete("farmer_roles", "`farmer_id` NOT IN (SELECT `farmer_id` FROM `farmers`)")
                quarantineAndDelete("farmer_roles", "CAST(`role_id` AS INTEGER) NOT IN (SELECT `role_id` FROM `roles`)")
                // Keep only one record per (farmer_id, role_id) if duplicates exist before enforcing UNIQUE(farmer_id, role_id)
                quarantineAndDelete(
                    "farmer_roles",
                    "`farmer_role_id` NOT IN (SELECT MIN(`farmer_role_id`) FROM `farmer_roles` GROUP BY `farmer_id`, `role_id`)"
                )

                quarantineTable("feed_crib_readings")
                quarantineAndDelete("feed_crib_readings", "`cribId` NOT IN (SELECT `id` FROM `feed_cribs`)")

                quarantineTable("feed_crib_reading_values")
                quarantineAndDelete("feed_crib_reading_values", "`readingId` NOT IN (SELECT `id` FROM `feed_crib_readings`)")

                quarantineTable("sync_backups")
                quarantineAndDelete("sync_backups", "`batchId` NOT IN (SELECT `id` FROM `sync_batches`)")

                // 2. Rebuild tables to declare foreign keys and indices

                // animals
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `animals_new` (
                        `animalId` TEXT NOT NULL,
                        `tagNumber` TEXT,
                        `oldTagNumber` TEXT,
                        `temperatureNumber` TEXT,
                        `referenceNumber` TEXT,
                        `massKg` REAL,
                        `birthdate` INTEGER NOT NULL,
                        `breed` TEXT NOT NULL,
                        `gender` TEXT,
                        `age` INTEGER,
                        `condition` TEXT,
                        `hideColour` TEXT,
                        `brandMark` TEXT,
                        `parentId` TEXT,
                        `animalGroupId` TEXT,
                        `photoPath` TEXT,
                        `videoPath` TEXT,
                        `gpsLat` REAL NOT NULL,
                        `gpsLng` REAL NOT NULL,
                        `captureAt` INTEGER NOT NULL,
                        `deviceId` TEXT NOT NULL,
                        `recordguid` TEXT NOT NULL,
                        `syncStatus` TEXT NOT NULL,
                        `syncedat` INTEGER,
                        PRIMARY KEY(`animalId`),
                        FOREIGN KEY(`animalGroupId`) REFERENCES `animal_groups`(`animalGroupId`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `animals_new` (
                        `animalId`, `tagNumber`, `oldTagNumber`, `temperatureNumber`, `referenceNumber`,
                        `massKg`, `birthdate`, `breed`, `gender`, `age`, `condition`, `hideColour`,
                        `brandMark`, `parentId`, `animalGroupId`, `photoPath`, `videoPath`, `gpsLat`,
                        `gpsLng`, `captureAt`, `deviceId`, `recordguid`, `syncStatus`, `syncedat`
                    )
                    SELECT
                        `animalId`, `tagNumber`, `oldTagNumber`, `temperatureNumber`, `referenceNumber`,
                        `massKg`, `birthdate`, `breed`, `gender`, `age`, `condition`, `hideColour`,
                        `brandMark`, `parentId`, `animalGroupId`, `photoPath`, `videoPath`, `gpsLat`,
                        `gpsLng`, `captureAt`, `deviceId`, `recordguid`, `syncStatus`, `syncedat`
                    FROM `animals`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `animals`")
                db.execSQL("ALTER TABLE `animals_new` RENAME TO `animals`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_tagNumber` ON `animals` (`tagNumber`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_temperatureNumber` ON `animals` (`temperatureNumber`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_parentId` ON `animals` (`parentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_animalGroupId` ON `animals` (`animalGroupId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animals_recordguid` ON `animals` (`recordguid`)")

                // treatments
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
                        `gpsLat` REAL NOT NULL,
                        `gpsLng` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `deviceId` TEXT NOT NULL DEFAULT '',
                        `recordguid` TEXT NOT NULL DEFAULT '',
                        `syncStatus` TEXT NOT NULL DEFAULT 'PENDING',
                        `syncedAt` INTEGER,
                        FOREIGN KEY(`animalId`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `treatments_new` (
                        `id`, `animalId`, `disease`, `treatmentName`, `batchNumber`, `volumeUsed`,
                        `cost`, `gpsLat`, `gpsLng`, `timestamp`, `deviceId`, `recordguid`, `syncStatus`, `syncedAt`
                    )
                    SELECT
                        `id`, `animalId`, `disease`, `treatmentName`, `batchNumber`, `volumeUsed`,
                        `cost`, `gpsLat`, `gpsLng`, `timestamp`, `deviceId`, `recordguid`, `syncStatus`, `syncedAt`
                    FROM `treatments`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `treatments`")
                db.execSQL("ALTER TABLE `treatments_new` RENAME TO `treatments`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_treatments_animalId` ON `treatments` (`animalId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_treatments_recordguid` ON `treatments` (`recordguid`)")

                // mortalities
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mortalities_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `animalId` TEXT NOT NULL,
                        `causeOfDeath` TEXT NOT NULL,
                        `responsibleWorker` TEXT NOT NULL DEFAULT '',
                        `notes` TEXT,
                        `timestamp` INTEGER NOT NULL,
                        `record_guid` TEXT NOT NULL,
                        FOREIGN KEY(`animalId`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `mortalities_new` (
                        `id`, `animalId`, `causeOfDeath`, `responsibleWorker`, `notes`, `timestamp`, `record_guid`
                    )
                    SELECT
                        `id`, `animalId`, `causeOfDeath`, `responsibleWorker`, `notes`, `timestamp`, `record_guid`
                    FROM `mortalities`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `mortalities`")
                db.execSQL("ALTER TABLE `mortalities_new` RENAME TO `mortalities`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_mortalities_animalId` ON `mortalities` (`animalId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_mortalities_record_guid` ON `mortalities` (`record_guid`)")

                // animal_group_memberships
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `animal_group_memberships_new` (
                        `membership_id` TEXT NOT NULL,
                        `animal_id` TEXT NOT NULL,
                        `group_id` TEXT NOT NULL,
                        `joined_at` INTEGER NOT NULL,
                        `left_at` INTEGER,
                        `record_guid` TEXT NOT NULL,
                        PRIMARY KEY(`membership_id`),
                        FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`group_id`) REFERENCES `animal_groups`(`animalGroupId`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `animal_group_memberships_new` (
                        `membership_id`, `animal_id`, `group_id`, `joined_at`, `left_at`, `record_guid`
                    )
                    SELECT
                        `membership_id`, `animal_id`, `group_id`, `joined_at`, `left_at`, `record_guid`
                    FROM `animal_group_memberships`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `animal_group_memberships`")
                db.execSQL("ALTER TABLE `animal_group_memberships_new` RENAME TO `animal_group_memberships`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_group_memberships_animal_id` ON `animal_group_memberships` (`animal_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_group_memberships_group_id` ON `animal_group_memberships` (`group_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_group_memberships_record_guid` ON `animal_group_memberships` (`record_guid`)")

                // farmer_addresses
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `farmer_addresses_new` (
                        `address_id` TEXT NOT NULL,
                        `farmer_id` TEXT NOT NULL,
                        `address_type` TEXT,
                        `address_line_1` TEXT,
                        `province` TEXT,
                        `postal_code` TEXT,
                        `gps_latitude` REAL,
                        `gps_longitude` REAL,
                        PRIMARY KEY(`address_id`),
                        FOREIGN KEY(`farmer_id`) REFERENCES `farmers`(`farmer_id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `farmer_addresses_new` (
                        `address_id`, `farmer_id`, `address_type`, `address_line_1`, `province`,
                        `postal_code`, `gps_latitude`, `gps_longitude`
                    )
                    SELECT
                        `address_id`, `farmer_id`, `address_type`, `address_line_1`, `province`,
                        `postal_code`, `gps_latitude`, `gps_longitude`
                    FROM `farmer_addresses`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `farmer_addresses`")
                db.execSQL("ALTER TABLE `farmer_addresses_new` RENAME TO `farmer_addresses`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_farmer_addresses_farmer_id` ON `farmer_addresses` (`farmer_id`)")

                // farmer_roles
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `farmer_roles_new` (
                        `farmer_role_id` TEXT NOT NULL,
                        `farmer_id` TEXT NOT NULL,
                        `role_id` INTEGER NOT NULL,
                        PRIMARY KEY(`farmer_role_id`),
                        FOREIGN KEY(`farmer_id`) REFERENCES `farmers`(`farmer_id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`role_id`) REFERENCES `roles`(`role_id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `farmer_roles_new` (
                        `farmer_role_id`, `farmer_id`, `role_id`
                    )
                    SELECT
                        `farmer_role_id`, `farmer_id`, CAST(`role_id` AS INTEGER)
                    FROM `farmer_roles`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `farmer_roles`")
                db.execSQL("ALTER TABLE `farmer_roles_new` RENAME TO `farmer_roles`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_farmer_roles_farmer_id` ON `farmer_roles` (`farmer_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_farmer_roles_role_id` ON `farmer_roles` (`role_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_farmer_roles_farmer_id_role_id` ON `farmer_roles` (`farmer_id`, `role_id`)")

                // feed_crib_readings
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `feed_crib_readings_new` (
                        `id` TEXT NOT NULL,
                        `cribId` TEXT NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`cribId`) REFERENCES `feed_cribs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `feed_crib_readings_new` (`id`, `cribId`)
                    SELECT `id`, `cribId` FROM `feed_crib_readings`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `feed_crib_readings`")
                db.execSQL("ALTER TABLE `feed_crib_readings_new` RENAME TO `feed_crib_readings`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_feed_crib_readings_cribId` ON `feed_crib_readings` (`cribId`)")

                // feed_crib_reading_values
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `feed_crib_reading_values_new` (
                        `id` TEXT NOT NULL,
                        `readingId` TEXT NOT NULL,
                        `value` REAL NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`readingId`) REFERENCES `feed_crib_readings`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `feed_crib_reading_values_new` (`id`, `readingId`, `value`)
                    SELECT `id`, `readingId`, `value` FROM `feed_crib_reading_values`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `feed_crib_reading_values`")
                db.execSQL("ALTER TABLE `feed_crib_reading_values_new` RENAME TO `feed_crib_reading_values`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_feed_crib_reading_values_readingId` ON `feed_crib_reading_values` (`readingId`)")

                // sync_backups
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_backups_new` (
                        `id` TEXT NOT NULL,
                        `batchId` TEXT NOT NULL,
                        `sync_status` TEXT NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`batchId`) REFERENCES `sync_batches`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `sync_backups_new` (`id`, `batchId`, `sync_status`)
                    SELECT `id`, `batchId`, `sync_status` FROM `sync_backups`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `sync_backups`")
                db.execSQL("ALTER TABLE `sync_backups_new` RENAME TO `sync_backups`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_backups_batchId` ON `sync_backups` (`batchId`)")

                // 3. Foreign Key Integrity Check
                db.query("PRAGMA foreign_key_check").use { check(it.count == 0) { "FK violations after 14->16" } }
            }
        }

        /**
         * D1 (Version 16 -> 17): calf_registrations becomes a sync-able event.
         * - Adds record_guid (UNIQUE), sync_status, synced_at
         * - Makes registered_animal_id UNIQUE (one registration per animal)
         * record_guid is set to registration_id for existing rows because that is the
         * value already sent to the backend as recordguid, so sync stays idempotent.
         */
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `calf_registrations_new` (`registration_id` TEXT NOT NULL, `registered_animal_id` TEXT NOT NULL, `dam_id` TEXT, `sire_id` TEXT, `birth_weight_kg` REAL, `calving_ease` TEXT, `registration_date` TEXT NOT NULL, `record_guid` TEXT NOT NULL, `sync_status` TEXT NOT NULL DEFAULT 'PENDING', `synced_at` INTEGER, PRIMARY KEY(`registration_id`), FOREIGN KEY(`registered_animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`dam_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`sire_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE SET NULL)")

                // Keep only the newest registration per animal so the new UNIQUE index cannot fail.
                db.execSQL("""
                    INSERT INTO `calf_registrations_new`
                        (`registration_id`, `registered_animal_id`, `dam_id`, `sire_id`,
                         `birth_weight_kg`, `calving_ease`, `registration_date`,
                         `record_guid`, `sync_status`, `synced_at`)
                    SELECT `registration_id`, `registered_animal_id`, `dam_id`, `sire_id`,
                           `birth_weight_kg`, `calving_ease`, `registration_date`,
                           `registration_id`, 'PENDING', NULL
                    FROM `calf_registrations` cr
                    WHERE cr.rowid = (SELECT MAX(rowid) FROM `calf_registrations`
                                      WHERE `registered_animal_id` = cr.`registered_animal_id`)
                """.trimIndent())

                db.execSQL("DROP TABLE `calf_registrations`")
                db.execSQL("ALTER TABLE `calf_registrations_new` RENAME TO `calf_registrations`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_calf_registrations_registered_animal_id` ON `calf_registrations` (`registered_animal_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calf_registrations_dam_id` ON `calf_registrations` (`dam_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calf_registrations_sire_id` ON `calf_registrations` (`sire_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_calf_registrations_record_guid` ON `calf_registrations` (`record_guid`)")
            }
        }

        /**
         * R0.6 (Version 17 -> 18): repair GUIDs left blank by migrations that
         * ran before D3/D6 were fixed, on devices that are already past v11.
         * - Gives a new UUID to every blank/NULL recordguid or record_guid on
         *   treatments, animals, animal_costs, mortalities, farmers and
         *   animal_group_memberships.
         * - For the one treatment whose recordguid was blank (at most one can
         *   exist, per the unique index added in 10->11), also points the
         *   derived TREATMENT cost row's source_record_id at the same new
         *   GUID, so the link the Phase 5 backfill relies on isn't left
         *   dangling on ''.
         */
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {

                fun backfillBlankGuids(table: String, column: String) {
                    val rowIds = mutableListOf<Long>()
                    db.query("SELECT `rowid` FROM `$table` WHERE `$column` IS NULL OR `$column` = ''").use { c ->
                        while (c.moveToNext()) rowIds += c.getLong(0)
                    }
                    rowIds.forEach { rowId ->
                        db.execSQL(
                            "UPDATE `$table` SET `$column` = ? WHERE `rowid` = ?",
                            arrayOf<Any>(UUID.randomUUID().toString(), rowId)
                        )
                    }
                }

                // Treatments first and specially: the matching animal_costs row
                // must be re-pointed at the same new GUID.
                val blankTreatmentIds = mutableListOf<Long>()
                db.query("SELECT `id` FROM `treatments` WHERE `recordguid` IS NULL OR `recordguid` = ''").use { c ->
                    while (c.moveToNext()) blankTreatmentIds += c.getLong(0)
                }
                check(blankTreatmentIds.size <= 1) {
                    "Expected at most one treatment with a blank recordguid (unique index " +
                            "since 10->11), found ${blankTreatmentIds.size}"
                }
                blankTreatmentIds.forEach { id ->
                    val newGuid = UUID.randomUUID().toString()
                    db.execSQL("UPDATE `treatments` SET `recordguid` = ? WHERE `id` = ?", arrayOf<Any>(newGuid, id))
                    db.execSQL(
                        "UPDATE `animal_costs` SET `source_record_id` = ? WHERE `source_entity` = 'TREATMENT' AND `source_record_id` = ''",
                        arrayOf<Any>(newGuid)
                    )
                }

                backfillBlankGuids("animals", "recordguid")
                backfillBlankGuids("animal_costs", "record_guid")
                backfillBlankGuids("mortalities", "record_guid")
                backfillBlankGuids("farmers", "record_guid")
                backfillBlankGuids("animal_group_memberships", "record_guid")
            }
        }

        /**
         * R3, bullet 1 (Version 18 -> 19): standardise the sync identity column.
         *
         * Renames `recordguid` to `record_guid` on `animals` and `treatments`, the
         * two syncable tables that still used the old, inconsistent name (every
         * other syncable table already uses `record_guid`, per rule 8). Both
         * tables are rebuilt per rule 9: exact DDL from `18.json` with the column
         * renamed, an explicit `INSERT ... SELECT`, then drop-and-rename. No data
         * is lost — every value carries over unchanged, only the column name and
         * the generated index name change.
         */
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {

                // 1. animals
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `animals_new` (`animalId` TEXT NOT NULL, `tagNumber` TEXT, `oldTagNumber` TEXT, `temperatureNumber` TEXT, `referenceNumber` TEXT, `massKg` REAL, `birthdate` INTEGER NOT NULL, `breed` TEXT NOT NULL, `gender` TEXT, `age` INTEGER, `condition` TEXT, `hideColour` TEXT, `brandMark` TEXT, `parentId` TEXT, `animalGroupId` TEXT, `photoPath` TEXT, `videoPath` TEXT, `gpsLat` REAL NOT NULL, `gpsLng` REAL NOT NULL, `captureAt` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `record_guid` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, `syncedat` INTEGER, PRIMARY KEY(`animalId`), FOREIGN KEY(`animalGroupId`) REFERENCES `animal_groups`(`animalGroupId`) ON UPDATE NO ACTION ON DELETE SET NULL )"
                )
                db.execSQL(
                    """
                    INSERT INTO `animals_new`
                        (`animalId`, `tagNumber`, `oldTagNumber`, `temperatureNumber`, `referenceNumber`,
                         `massKg`, `birthdate`, `breed`, `gender`, `age`, `condition`, `hideColour`,
                         `brandMark`, `parentId`, `animalGroupId`, `photoPath`, `videoPath`, `gpsLat`,
                         `gpsLng`, `captureAt`, `deviceId`, `record_guid`, `syncStatus`, `syncedat`)
                    SELECT
                        `animalId`, `tagNumber`, `oldTagNumber`, `temperatureNumber`, `referenceNumber`,
                        `massKg`, `birthdate`, `breed`, `gender`, `age`, `condition`, `hideColour`,
                        `brandMark`, `parentId`, `animalGroupId`, `photoPath`, `videoPath`, `gpsLat`,
                        `gpsLng`, `captureAt`, `deviceId`, `recordguid`, `syncStatus`, `syncedat`
                    FROM `animals`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `animals`")
                db.execSQL("ALTER TABLE `animals_new` RENAME TO `animals`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_tagNumber` ON `animals` (`tagNumber`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_temperatureNumber` ON `animals` (`temperatureNumber`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_parentId` ON `animals` (`parentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_animalGroupId` ON `animals` (`animalGroupId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animals_record_guid` ON `animals` (`record_guid`)")

                // 2. treatments
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `treatments_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `animalId` TEXT NOT NULL, `disease` TEXT NOT NULL, `treatmentName` TEXT NOT NULL, `batchNumber` TEXT NOT NULL, `volumeUsed` TEXT NOT NULL, `cost` REAL NOT NULL, `gpsLat` REAL NOT NULL, `gpsLng` REAL NOT NULL, `timestamp` INTEGER NOT NULL, `deviceId` TEXT NOT NULL DEFAULT '', `record_guid` TEXT NOT NULL DEFAULT '', `syncStatus` TEXT NOT NULL DEFAULT 'PENDING', `syncedAt` INTEGER, FOREIGN KEY(`animalId`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    """
                    INSERT INTO `treatments_new`
                        (`id`, `animalId`, `disease`, `treatmentName`, `batchNumber`, `volumeUsed`,
                         `cost`, `gpsLat`, `gpsLng`, `timestamp`, `deviceId`, `record_guid`,
                         `syncStatus`, `syncedAt`)
                    SELECT
                        `id`, `animalId`, `disease`, `treatmentName`, `batchNumber`, `volumeUsed`,
                        `cost`, `gpsLat`, `gpsLng`, `timestamp`, `deviceId`, `recordguid`,
                        `syncStatus`, `syncedAt`
                    FROM `treatments`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `treatments`")
                db.execSQL("ALTER TABLE `treatments_new` RENAME TO `treatments`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_treatments_animalId` ON `treatments` (`animalId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_treatments_record_guid` ON `treatments` (`record_guid`)")
            }
        }

        /**
         * R3, bullet 2 (Version 19 -> 20): give every remaining syncable table a
         * `record_guid`.
         *
         * `animal_costs`, `mortalities`, `farmers`, `animal_group_memberships`,
         * `calf_registrations`, `animals` and `treatments` already have one.
         * This adds it to the rest: `animal_movements`, `animal_weights`,
         * `animal_identifiers`, `animal_media`, `animal_ownerships`,
         * `animal_purchases`, `farmer_addresses`, `feed_crib_readings` and
         * `feed_crib_reading_values`. Each column is added with a transient
         * `DEFAULT ''` (SQLite requires one for a `NOT NULL` `ADD COLUMN`),
         * every existing row is immediately given a real UUID so nothing keeps
         * the blank default, then the unique index is created.
         */
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {

                val tables = listOf(
                    "animal_movements",
                    "animal_weights",
                    "animal_identifiers",
                    "animal_media",
                    "animal_ownerships",
                    "animal_purchases",
                    "farmer_addresses",
                    "feed_crib_readings",
                    "feed_crib_reading_values"
                )

                tables.forEach { table ->
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `record_guid` TEXT NOT NULL DEFAULT ''")

                    val rowIds = mutableListOf<Long>()
                    db.query("SELECT `rowid` FROM `$table` WHERE `record_guid` = ''").use { c ->
                        while (c.moveToNext()) rowIds += c.getLong(0)
                    }
                    rowIds.forEach { rowId ->
                        db.execSQL(
                            "UPDATE `$table` SET `record_guid` = ? WHERE `rowid` = ?",
                            arrayOf<Any>(UUID.randomUUID().toString(), rowId)
                        )
                    }

                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_${table}_record_guid` ON `$table` (`record_guid`)"
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
                RoleSeed.execute(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                CostTypeSeed.execute(db)
                RoleSeed.execute(db)
            }
        }
    }
}
