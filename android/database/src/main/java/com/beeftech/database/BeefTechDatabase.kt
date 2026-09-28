package com.beeftech.database

import android.database.Cursor
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.text.SimpleDateFormat
import java.util.Locale
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

// Phase 4 Lookup Entities
import com.beeftech.database.entity.Breed
import com.beeftech.database.entity.HideColour
import com.beeftech.database.entity.NecropsyCode
import com.beeftech.database.entity.Disease
import com.beeftech.database.entity.Medication
import com.beeftech.database.entity.MedicationBatch
import com.beeftech.database.entity.Country
import com.beeftech.database.entity.Province
import com.beeftech.database.entity.Device

// Phase 0 / R4 Lookup Entity
import com.beeftech.database.entity.IdentifierType

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
import com.beeftech.database.dao.BreedDao
import com.beeftech.database.dao.HideColourDao
import com.beeftech.database.dao.NecropsyCodeDao
import com.beeftech.database.dao.DiseaseDao
import com.beeftech.database.dao.MedicationDao
import com.beeftech.database.dao.MedicationBatchDao
import com.beeftech.database.dao.CountryDao
import com.beeftech.database.dao.ProvinceDao
import com.beeftech.database.dao.DeviceDao
import com.beeftech.database.dao.IdentifierTypeDao
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
        CostType::class,

        // Phase 4 Lookup Entities
        Breed::class,
        HideColour::class,
        NecropsyCode::class,
        Disease::class,
        Medication::class,
        MedicationBatch::class,
        Country::class,
        Province::class,
        Device::class,

        // Phase 0 / R4 Lookup Entity
        IdentifierType::class
    ],
    version = 28,
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

    // Phase 4 Lookup DAOs
    abstract fun breedDao(): BreedDao
    abstract fun hideColourDao(): HideColourDao
    abstract fun necropsyCodeDao(): NecropsyCodeDao
    abstract fun diseaseDao(): DiseaseDao
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationBatchDao(): MedicationBatchDao
    abstract fun countryDao(): CountryDao
    abstract fun provinceDao(): ProvinceDao
    abstract fun deviceDao(): DeviceDao
    abstract fun identifierTypeDao(): IdentifierTypeDao


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

        /**
         * R3, bullet 3 (Version 20 -> 21): audit fields on `animal_movements`
         * and `animal_weights`.
         *
         * Adds `gps_lat`, `gps_lng`, `device_id`, `captured_at`, `sync_status`
         * and `synced_at` to both tables, each with a transient default (same
         * reasoning as `record_guid` in MIGRATION_19_20).
         *
         * `captured_at` is backfilled from `movement_date` on every
         * `animal_movements` row: `movement_date` has always held either
         * `System.currentTimeMillis().toString()` (rows written directly by
         * the app) or `CAST(timestamp AS TEXT)` (rows recovered from
         * `legacy_animal_movements` by MIGRATION_10_11), so it is already a
         * numeric string on every row.
         *
         * `gps_lat`, `gps_lng`, `device_id`, `sync_status` and `synced_at`
         * cannot be recovered that way -- nothing before this point ever
         * stored them for `animal_movements` -- but `legacy_animal_movements`
         * (created by MIGRATION_10_11, only on devices that upgraded from
         * v10) still has the original v10 values, keyed by `recordguid`,
         * which R0.2 carried forward unchanged as the new `movement_id`. A
         * device that never went through that migration, or a fresh install,
         * has no `legacy_animal_movements` table at all, so this is guarded.
         *
         * `animal_weights` has no legacy predecessor -- it was introduced at
         * v9 -> v10 without any of these fields -- so its rows simply keep
         * the defaults.
         */
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {

                fun addAuditColumns(table: String) {
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `gps_lat` REAL NOT NULL DEFAULT 0.0")
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `gps_lng` REAL NOT NULL DEFAULT 0.0")
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `device_id` TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `captured_at` INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `sync_status` TEXT NOT NULL DEFAULT 'PENDING'")
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `synced_at` INTEGER")
                }

                addAuditColumns("animal_movements")
                addAuditColumns("animal_weights")

                // Every animal_movements row's movement_date is already a
                // numeric millisecond string (see the migration doc above).
                db.execSQL(
                    "UPDATE `animal_movements` SET `captured_at` = CAST(`movement_date` AS INTEGER)"
                )

                val legacyTableExists = db.query(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'legacy_animal_movements'"
                ).use { c ->
                    c.moveToFirst() && c.getInt(0) > 0
                }

                if (legacyTableExists) {
                    db.execSQL(
                        """
                        UPDATE `animal_movements`
                        SET
                            `gps_lat` = (
                                SELECT `gpsLat` FROM `legacy_animal_movements`
                                WHERE `legacy_animal_movements`.`recordguid` = `animal_movements`.`movement_id`
                            ),
                            `gps_lng` = (
                                SELECT `gpsLng` FROM `legacy_animal_movements`
                                WHERE `legacy_animal_movements`.`recordguid` = `animal_movements`.`movement_id`
                            ),
                            `device_id` = (
                                SELECT `deviceId` FROM `legacy_animal_movements`
                                WHERE `legacy_animal_movements`.`recordguid` = `animal_movements`.`movement_id`
                            ),
                            `sync_status` = (
                                SELECT `syncStatus` FROM `legacy_animal_movements`
                                WHERE `legacy_animal_movements`.`recordguid` = `animal_movements`.`movement_id`
                            ),
                            `synced_at` = (
                                SELECT `syncedAt` FROM `legacy_animal_movements`
                                WHERE `legacy_animal_movements`.`recordguid` = `animal_movements`.`movement_id`
                            )
                        WHERE EXISTS (
                            SELECT 1 FROM `legacy_animal_movements`
                            WHERE `legacy_animal_movements`.`recordguid` = `animal_movements`.`movement_id`
                        )
                        """.trimIndent()
                    )
                }
            }
        }

        /**
         * A column in a date-column table rebuild (see [MIGRATION_21_22]).
         * Non-date columns are copied through unchanged, whatever their
         * runtime type; a date column's TEXT value is parsed to epoch
         * milliseconds ([dateNullable] controls whether a blank/unparseable
         * value becomes SQL NULL or falls back to 0).
         */
        private data class DateRebuildColumn(
            val name: String,
            val isDateColumn: Boolean = false,
            val dateNullable: Boolean = false
        )

        /**
         * Parses a date string that is either already a millisecond epoch
         * (e.g. `animal_movements.movement_date`, always
         * `System.currentTimeMillis().toString()`) or an ISO date/datetime
         * string produced by `CalfRegistrationMappers.isoDate` (`yyyy-MM-dd`)
         * or a test fixture (`yyyy-MM-dd'T'HH:mm:ss[.SSS]`). Returns null for
         * a blank, null, or genuinely unparseable value -- never a fabricated
         * date (rule: never invent values).
         */
        private fun parseDateStringToEpochMillis(value: String?): Long? {
            if (value.isNullOrBlank()) return null

            value.toLongOrNull()?.let { return it }

            val patterns = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
            )
            for (pattern in patterns) {
                try {
                    val format = SimpleDateFormat(pattern, Locale.US)
                    format.isLenient = false
                    format.parse(value)?.let { return it.time }
                } catch (_: Exception) {
                    // Try the next pattern.
                }
            }
            return null
        }

        /**
         * R3, bullet 5 (Version 21 -> 22): convert TEXT date columns to
         * epoch-millisecond INTEGER, on the seven tables that still store
         * one: `animal_movements.movement_date`, `animal_identifiers.valid_from`
         * / `valid_to`, `animal_media.created_at`, `animal_weights.weigh_date`,
         * `animal_ownerships.start_date` / `end_date`,
         * `animal_purchases.purchase_date` and `calf_registrations.registration_date`.
         *
         * Each table is rebuilt per rule 9. Retyping an existing column can't
         * be done with `ALTER TABLE`, and parsing mixed ISO/numeric strings
         * needs real code, not a SQL expression, so every row is read,
         * converted in Kotlin via [parseDateStringToEpochMillis], and
         * re-inserted with an explicit column list.
         */
        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {

                fun rebuildTable(
                    table: String,
                    createNewTableSql: String,
                    columns: List<DateRebuildColumn>,
                    indexSqls: List<String>
                ) {
                    val newTable = "${table}_new"
                    db.execSQL(createNewTableSql)

                    val selectCols = columns.joinToString(", ") { "`${it.name}`" }
                    val insertCols = columns.joinToString(", ") { "`${it.name}`" }
                    val placeholders = columns.joinToString(", ") { "?" }

                    db.query("SELECT $selectCols FROM `$table`").use { c ->
                        while (c.moveToNext()) {
                            val values = columns.mapIndexed { i, column ->
                                if (column.isDateColumn) {
                                    val raw = if (c.isNull(i)) null else c.getString(i)
                                    val parsed = parseDateStringToEpochMillis(raw)
                                    if (column.dateNullable) parsed else (parsed ?: 0L)
                                } else if (c.isNull(i)) {
                                    null
                                } else {
                                    when (c.getType(i)) {
                                        Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                                        Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
                                        else -> c.getString(i)
                                    }
                                }
                            }
                            db.execSQL(
                                "INSERT INTO `$newTable` ($insertCols) VALUES ($placeholders)",
                                values.toTypedArray()
                            )
                        }
                    }

                    db.execSQL("DROP TABLE `$table`")
                    db.execSQL("ALTER TABLE `$newTable` RENAME TO `$table`")
                    indexSqls.forEach { db.execSQL(it) }
                }

                rebuildTable(
                    table = "animal_movements",
                    createNewTableSql = "CREATE TABLE IF NOT EXISTS `animal_movements_new` (`movement_id` TEXT NOT NULL, `animal_id` TEXT NOT NULL, `source_farm_id` TEXT, `source_pen_id` TEXT, `destination_farm_id` TEXT NOT NULL, `destination_pen_id` TEXT NOT NULL, `movement_date` INTEGER NOT NULL, `feed_location_type` TEXT, `notes` TEXT, `record_guid` TEXT NOT NULL DEFAULT '', `gps_lat` REAL NOT NULL DEFAULT 0.0, `gps_lng` REAL NOT NULL DEFAULT 0.0, `device_id` TEXT NOT NULL DEFAULT '', `captured_at` INTEGER NOT NULL DEFAULT 0, `sync_status` TEXT NOT NULL DEFAULT 'PENDING', `synced_at` INTEGER, PRIMARY KEY(`movement_id`), FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                    columns = listOf(
                        DateRebuildColumn("movement_id"),
                        DateRebuildColumn("animal_id"),
                        DateRebuildColumn("source_farm_id"),
                        DateRebuildColumn("source_pen_id"),
                        DateRebuildColumn("destination_farm_id"),
                        DateRebuildColumn("destination_pen_id"),
                        DateRebuildColumn("movement_date", isDateColumn = true),
                        DateRebuildColumn("feed_location_type"),
                        DateRebuildColumn("notes"),
                        DateRebuildColumn("record_guid"),
                        DateRebuildColumn("gps_lat"),
                        DateRebuildColumn("gps_lng"),
                        DateRebuildColumn("device_id"),
                        DateRebuildColumn("captured_at"),
                        DateRebuildColumn("sync_status"),
                        DateRebuildColumn("synced_at")
                    ),
                    indexSqls = listOf(
                        "CREATE INDEX IF NOT EXISTS `index_animal_movements_animal_id` ON `animal_movements` (`animal_id`)",
                        "CREATE INDEX IF NOT EXISTS `index_animal_movements_destination_farm_id` ON `animal_movements` (`destination_farm_id`)",
                        "CREATE INDEX IF NOT EXISTS `index_animal_movements_destination_pen_id` ON `animal_movements` (`destination_pen_id`)",
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_movements_record_guid` ON `animal_movements` (`record_guid`)"
                    )
                )

                rebuildTable(
                    table = "animal_identifiers",
                    createNewTableSql = "CREATE TABLE IF NOT EXISTS `animal_identifiers_new` (`identifier_id` TEXT NOT NULL, `animal_id` TEXT NOT NULL, `identifier_type` TEXT NOT NULL, `identifier_value` TEXT NOT NULL, `valid_from` INTEGER, `valid_to` INTEGER, `record_guid` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`identifier_id`), FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                    columns = listOf(
                        DateRebuildColumn("identifier_id"),
                        DateRebuildColumn("animal_id"),
                        DateRebuildColumn("identifier_type"),
                        DateRebuildColumn("identifier_value"),
                        DateRebuildColumn("valid_from", isDateColumn = true, dateNullable = true),
                        DateRebuildColumn("valid_to", isDateColumn = true, dateNullable = true),
                        DateRebuildColumn("record_guid")
                    ),
                    indexSqls = listOf(
                        "CREATE INDEX IF NOT EXISTS `index_animal_identifiers_animal_id` ON `animal_identifiers` (`animal_id`)",
                        "CREATE INDEX IF NOT EXISTS `index_animal_identifiers_identifier_type_identifier_value` ON `animal_identifiers` (`identifier_type`, `identifier_value`)",
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_identifiers_record_guid` ON `animal_identifiers` (`record_guid`)"
                    )
                )

                rebuildTable(
                    table = "animal_media",
                    createNewTableSql = "CREATE TABLE IF NOT EXISTS `animal_media_new` (`media_id` TEXT NOT NULL, `animal_id` TEXT NOT NULL, `file_path` TEXT NOT NULL, `media_type` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `record_guid` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`media_id`), FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                    columns = listOf(
                        DateRebuildColumn("media_id"),
                        DateRebuildColumn("animal_id"),
                        DateRebuildColumn("file_path"),
                        DateRebuildColumn("media_type"),
                        DateRebuildColumn("created_at", isDateColumn = true),
                        DateRebuildColumn("record_guid")
                    ),
                    indexSqls = listOf(
                        "CREATE INDEX IF NOT EXISTS `index_animal_media_animal_id` ON `animal_media` (`animal_id`)",
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_media_record_guid` ON `animal_media` (`record_guid`)"
                    )
                )

                rebuildTable(
                    table = "animal_weights",
                    createNewTableSql = "CREATE TABLE IF NOT EXISTS `animal_weights_new` (`weight_id` TEXT NOT NULL, `animal_id` TEXT NOT NULL, `weight_kg` REAL NOT NULL, `weigh_date` INTEGER NOT NULL, `notes` TEXT, `record_guid` TEXT NOT NULL DEFAULT '', `gps_lat` REAL NOT NULL DEFAULT 0.0, `gps_lng` REAL NOT NULL DEFAULT 0.0, `device_id` TEXT NOT NULL DEFAULT '', `captured_at` INTEGER NOT NULL DEFAULT 0, `sync_status` TEXT NOT NULL DEFAULT 'PENDING', `synced_at` INTEGER, PRIMARY KEY(`weight_id`), FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                    columns = listOf(
                        DateRebuildColumn("weight_id"),
                        DateRebuildColumn("animal_id"),
                        DateRebuildColumn("weight_kg"),
                        DateRebuildColumn("weigh_date", isDateColumn = true),
                        DateRebuildColumn("notes"),
                        DateRebuildColumn("record_guid"),
                        DateRebuildColumn("gps_lat"),
                        DateRebuildColumn("gps_lng"),
                        DateRebuildColumn("device_id"),
                        DateRebuildColumn("captured_at"),
                        DateRebuildColumn("sync_status"),
                        DateRebuildColumn("synced_at")
                    ),
                    indexSqls = listOf(
                        "CREATE INDEX IF NOT EXISTS `index_animal_weights_animal_id` ON `animal_weights` (`animal_id`)",
                        "CREATE INDEX IF NOT EXISTS `index_animal_weights_weigh_date` ON `animal_weights` (`weigh_date`)",
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_weights_record_guid` ON `animal_weights` (`record_guid`)"
                    )
                )

                rebuildTable(
                    table = "animal_ownerships",
                    createNewTableSql = "CREATE TABLE IF NOT EXISTS `animal_ownerships_new` (`ownership_id` TEXT NOT NULL, `animal_id` TEXT NOT NULL, `owner_name` TEXT NOT NULL, `ownership_percentage` REAL NOT NULL, `start_date` INTEGER NOT NULL, `end_date` INTEGER, `record_guid` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`ownership_id`), FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                    columns = listOf(
                        DateRebuildColumn("ownership_id"),
                        DateRebuildColumn("animal_id"),
                        DateRebuildColumn("owner_name"),
                        DateRebuildColumn("ownership_percentage"),
                        DateRebuildColumn("start_date", isDateColumn = true),
                        DateRebuildColumn("end_date", isDateColumn = true, dateNullable = true),
                        DateRebuildColumn("record_guid")
                    ),
                    indexSqls = listOf(
                        "CREATE INDEX IF NOT EXISTS `index_animal_ownerships_animal_id` ON `animal_ownerships` (`animal_id`)",
                        "CREATE INDEX IF NOT EXISTS `index_animal_ownerships_owner_name` ON `animal_ownerships` (`owner_name`)",
                        "CREATE INDEX IF NOT EXISTS `index_animal_ownerships_start_date` ON `animal_ownerships` (`start_date`)",
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_ownerships_record_guid` ON `animal_ownerships` (`record_guid`)"
                    )
                )

                rebuildTable(
                    table = "animal_purchases",
                    createNewTableSql = "CREATE TABLE IF NOT EXISTS `animal_purchases_new` (`purchase_id` TEXT NOT NULL, `animal_id` TEXT NOT NULL, `purchase_price` REAL NOT NULL, `purchase_date` INTEGER NOT NULL, `seller_name` TEXT NOT NULL, `notes` TEXT, `record_guid` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`purchase_id`), FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                    columns = listOf(
                        DateRebuildColumn("purchase_id"),
                        DateRebuildColumn("animal_id"),
                        DateRebuildColumn("purchase_price"),
                        DateRebuildColumn("purchase_date", isDateColumn = true),
                        DateRebuildColumn("seller_name"),
                        DateRebuildColumn("notes"),
                        DateRebuildColumn("record_guid")
                    ),
                    indexSqls = listOf(
                        "CREATE INDEX IF NOT EXISTS `index_animal_purchases_animal_id` ON `animal_purchases` (`animal_id`)",
                        "CREATE INDEX IF NOT EXISTS `index_animal_purchases_purchase_date` ON `animal_purchases` (`purchase_date`)",
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_purchases_record_guid` ON `animal_purchases` (`record_guid`)"
                    )
                )

                rebuildTable(
                    table = "calf_registrations",
                    createNewTableSql = "CREATE TABLE IF NOT EXISTS `calf_registrations_new` (`registration_id` TEXT NOT NULL, `registered_animal_id` TEXT NOT NULL, `dam_id` TEXT, `sire_id` TEXT, `birth_weight_kg` REAL, `calving_ease` TEXT, `registration_date` INTEGER NOT NULL, `record_guid` TEXT NOT NULL, `sync_status` TEXT NOT NULL DEFAULT 'PENDING', `synced_at` INTEGER, PRIMARY KEY(`registration_id`), FOREIGN KEY(`registered_animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`dam_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`sire_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE SET NULL)",
                    columns = listOf(
                        DateRebuildColumn("registration_id"),
                        DateRebuildColumn("registered_animal_id"),
                        DateRebuildColumn("dam_id"),
                        DateRebuildColumn("sire_id"),
                        DateRebuildColumn("birth_weight_kg"),
                        DateRebuildColumn("calving_ease"),
                        DateRebuildColumn("registration_date", isDateColumn = true),
                        DateRebuildColumn("record_guid"),
                        DateRebuildColumn("sync_status"),
                        DateRebuildColumn("synced_at")
                    ),
                    indexSqls = listOf(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_calf_registrations_registered_animal_id` ON `calf_registrations` (`registered_animal_id`)",
                        "CREATE INDEX IF NOT EXISTS `index_calf_registrations_dam_id` ON `calf_registrations` (`dam_id`)",
                        "CREATE INDEX IF NOT EXISTS `index_calf_registrations_sire_id` ON `calf_registrations` (`sire_id`)",
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_calf_registrations_record_guid` ON `calf_registrations` (`record_guid`)"
                    )
                )
            }
        }

        /**
         * R5.1 Migration (Version 22 -> 23): add the missing foreign key from
         * `animal_costs.animalId` to `animals`.
         *
         * `animal_costs` was created in MIGRATION_11_12 (after MIGRATION_14_16's
         * FK/orphan-cleanup pass already ran on devices past v14) and rebuilt in
         * MIGRATION_13_14 to add the `costType` FK -- but nothing ever swept
         * `animalId` the way MIGRATION_14_16 swept treatments, mortalities and
         * animal_group_memberships. This closes that gap.
         *
         * Follows the same re-key-before-quarantine shape as MIGRATION_14_16:
         * a row keyed on a tag by a build before D1 gets one chance to resolve
         * to the animal's UUID before it's treated as an orphan (rule 3).
         */
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {

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

                rekeyTagToAnimalId("animal_costs", "animalId")
                quarantineTable("animal_costs")
                quarantineAndDelete("animal_costs", "`animalId` NOT IN (SELECT `animalId` FROM `animals`)")

                // Rule 9: rebuild with the exact DDL from 22.json, plus the new FK.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `animal_costs_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `animalId` TEXT NOT NULL,
                        `costType` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `gpsLat` REAL NOT NULL,
                        `gpsLng` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `source_entity` TEXT,
                        `source_record_id` TEXT,
                        `record_guid` TEXT NOT NULL,
                        FOREIGN KEY(`costType`) REFERENCES `cost_types`(`code`) ON UPDATE CASCADE ON DELETE RESTRICT,
                        FOREIGN KEY(`animalId`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `animal_costs_new` (
                        `id`, `animalId`, `costType`, `amount`, `description`, `gpsLat`, `gpsLng`,
                        `timestamp`, `source_entity`, `source_record_id`, `record_guid`
                    )
                    SELECT
                        `id`, `animalId`, `costType`, `amount`, `description`, `gpsLat`, `gpsLng`,
                        `timestamp`, `source_entity`, `source_record_id`, `record_guid`
                    FROM `animal_costs`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `animal_costs`")
                db.execSQL("ALTER TABLE `animal_costs_new` RENAME TO `animal_costs`")

                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_costs_record_guid` ON `animal_costs` (`record_guid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_costs_costType` ON `animal_costs` (`costType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_costs_animalId_costType_timestamp` ON `animal_costs` (`animalId`, `costType`, `timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_costs_animalId` ON `animal_costs` (`animalId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_costs_source_entity_source_record_id` ON `animal_costs` (`source_entity`, `source_record_id`)")

                // Rule 10: verify both animal_costs FKs (costType and the new animalId) hold.
                db.query("PRAGMA foreign_key_check").use { check(it.count == 0) { "FK violations after 22->23" } }
            }
        }

        /**
         * 23 -> 24 (R7: Controlled vocabulary lookup tables and compliance fields)
         */
        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create lookup tables
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `breeds` (
                        `breedId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        PRIMARY KEY(`breedId`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `hide_colours` (
                        `colourId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        PRIMARY KEY(`colourId`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `necropsy_codes` (
                        `necropsyCodeId` TEXT NOT NULL,
                        `code` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        PRIMARY KEY(`necropsyCodeId`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `diseases` (
                        `diseaseId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        PRIMARY KEY(`diseaseId`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `medications` (
                        `medicationId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `withdrawal_period_days` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`medicationId`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `medication_batches` (
                        `batchId` TEXT NOT NULL,
                        `medicationId` TEXT NOT NULL,
                        `batch_number` TEXT NOT NULL,
                        `expiry_date` INTEGER,
                        PRIMARY KEY(`batchId`),
                        FOREIGN KEY(`medicationId`) REFERENCES `medications`(`medicationId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_medication_batches_medicationId` ON `medication_batches` (`medicationId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_medication_batches_batch_number` ON `medication_batches` (`batch_number`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `countries` (
                        `countryId` TEXT NOT NULL,
                        `iso_code` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        PRIMARY KEY(`countryId`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `provinces` (
                        `provinceId` TEXT NOT NULL,
                        `countryId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        PRIMARY KEY(`provinceId`),
                        FOREIGN KEY(`countryId`) REFERENCES `countries`(`countryId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_provinces_countryId` ON `provinces` (`countryId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `devices` (
                        `deviceId` TEXT NOT NULL,
                        `device_assigned_id` TEXT,
                        `model` TEXT,
                        `last_sync` INTEGER,
                        PRIMARY KEY(`deviceId`)
                    )
                    """.trimIndent()
                )

                // 2. Execute Lookup Seeds
                BreedSeed.execute(db)
                HideColourSeed.execute(db)
                DiseaseSeed.execute(db)
                MedicationSeed.execute(db)
                CountryProvinceSeed.execute(db)
                NecropsyCodeSeed.execute(db)

                // 3. Backfill devices from existing tables
                db.execSQL("INSERT OR IGNORE INTO `devices` (`deviceId`) VALUES ('')")
                db.execSQL("INSERT OR IGNORE INTO `devices` (`deviceId`) SELECT DISTINCT `deviceId` FROM `animals` WHERE `deviceId` IS NOT NULL AND `deviceId` != ''")
                db.execSQL("INSERT OR IGNORE INTO `devices` (`deviceId`) SELECT DISTINCT `deviceId` FROM `treatments` WHERE `deviceId` IS NOT NULL AND `deviceId` != ''")
                db.execSQL("INSERT OR IGNORE INTO `devices` (`deviceId`) SELECT DISTINCT `device_id` FROM `animal_movements` WHERE `device_id` IS NOT NULL AND `device_id` != ''")
                db.execSQL("INSERT OR IGNORE INTO `devices` (`deviceId`) SELECT DISTINCT `device_id` FROM `animal_weights` WHERE `device_id` IS NOT NULL AND `device_id` != ''")

                // Backfill breeds, hide colours, diseases, provinces from existing tables if needed
                db.execSQL("INSERT OR IGNORE INTO `breeds` (`breedId`, `name`) SELECT DISTINCT `breed`, `breed` FROM `animals` WHERE `breed` IS NOT NULL AND `breed` != ''")
                db.execSQL("INSERT OR IGNORE INTO `hide_colours` (`colourId`, `name`) SELECT DISTINCT `hideColour`, `hideColour` FROM `animals` WHERE `hideColour` IS NOT NULL AND `hideColour` != ''")
                db.execSQL("INSERT OR IGNORE INTO `diseases` (`diseaseId`, `name`) SELECT DISTINCT `disease`, `disease` FROM `treatments` WHERE `disease` IS NOT NULL AND `disease` != ''")
                db.execSQL("INSERT OR IGNORE INTO `provinces` (`provinceId`, `countryId`, `name`) SELECT DISTINCT `province`, 'ZAF', `province` FROM `farmer_addresses` WHERE `province` IS NOT NULL AND `province` != ''")

                // 4. Rebuild animals table with FKs
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
                        `record_guid` TEXT NOT NULL,
                        `syncStatus` TEXT NOT NULL,
                        `syncedat` INTEGER,
                        PRIMARY KEY(`animalId`),
                        FOREIGN KEY(`animalGroupId`) REFERENCES `animal_groups`(`animalGroupId`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`breed`) REFERENCES `breeds`(`breedId`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`hideColour`) REFERENCES `hide_colours`(`colourId`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`deviceId`) REFERENCES `devices`(`deviceId`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `animals_new` (
                        `animalId`, `tagNumber`, `oldTagNumber`, `temperatureNumber`, `referenceNumber`,
                        `massKg`, `birthdate`, `breed`, `gender`, `age`, `condition`, `hideColour`,
                        `brandMark`, `parentId`, `animalGroupId`, `photoPath`, `videoPath`, `gpsLat`,
                        `gpsLng`, `captureAt`, `deviceId`, `record_guid`, `syncStatus`, `syncedat`
                    )
                    SELECT
                        `animalId`, `tagNumber`, `oldTagNumber`, `temperatureNumber`, `referenceNumber`,
                        `massKg`, `birthdate`, `breed`, `gender`, `age`, `condition`, `hideColour`,
                        `brandMark`, `parentId`, `animalGroupId`, `photoPath`, `videoPath`, `gpsLat`,
                        `gpsLng`, `captureAt`, `deviceId`, `record_guid`, `syncStatus`, `syncedat`
                    FROM `animals`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `animals`")
                db.execSQL("ALTER TABLE `animals_new` RENAME TO `animals`")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_tagNumber` ON `animals` (`tagNumber`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_temperatureNumber` ON `animals` (`temperatureNumber`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_parentId` ON `animals` (`parentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_animalGroupId` ON `animals` (`animalGroupId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_breed` ON `animals` (`breed`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_hideColour` ON `animals` (`hideColour`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_deviceId` ON `animals` (`deviceId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animals_record_guid` ON `animals` (`record_guid`)")

                // 5. Rebuild treatments table
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
                        `withdrawal_clear_date` INTEGER,
                        `record_guid` TEXT NOT NULL DEFAULT '',
                        `syncStatus` TEXT NOT NULL DEFAULT 'PENDING',
                        `syncedAt` INTEGER,
                        FOREIGN KEY(`animalId`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`disease`) REFERENCES `diseases`(`diseaseId`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`deviceId`) REFERENCES `devices`(`deviceId`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `treatments_new` (
                        `id`, `animalId`, `disease`, `treatmentName`, `batchNumber`, `volumeUsed`,
                        `cost`, `gpsLat`, `gpsLng`, `timestamp`, `deviceId`, `record_guid`,
                        `syncStatus`, `syncedAt`
                    )
                    SELECT
                        `id`, `animalId`, `disease`, `treatmentName`, `batchNumber`, `volumeUsed`,
                        `cost`, `gpsLat`, `gpsLng`, `timestamp`, `deviceId`, `record_guid`,
                        `syncStatus`, `syncedAt`
                    FROM `treatments`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `treatments`")
                db.execSQL("ALTER TABLE `treatments_new` RENAME TO `treatments`")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_treatments_animalId` ON `treatments` (`animalId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_treatments_disease` ON `treatments` (`disease`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_treatments_deviceId` ON `treatments` (`deviceId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_treatments_record_guid` ON `treatments` (`record_guid`)")

                // 6. Rebuild mortalities table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mortalities_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `animalId` TEXT NOT NULL,
                        `causeOfDeath` TEXT NOT NULL,
                        `necropsy_code_id` TEXT,
                        `responsibleWorker` TEXT NOT NULL DEFAULT '',
                        `notes` TEXT,
                        `timestamp` INTEGER NOT NULL,
                        `record_guid` TEXT NOT NULL,
                        FOREIGN KEY(`animalId`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`necropsy_code_id`) REFERENCES `necropsy_codes`(`necropsyCodeId`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `mortalities_new` (
                        `id`, `animalId`, `causeOfDeath`, `necropsy_code_id`, `responsibleWorker`, `notes`, `timestamp`, `record_guid`
                    )
                    SELECT
                        `id`, `animalId`, `causeOfDeath`,
                        CASE
                            WHEN `causeOfDeath` IN (SELECT `necropsyCodeId` FROM `necropsy_codes`) THEN `causeOfDeath`
                            WHEN `causeOfDeath` IN (SELECT `code` FROM `necropsy_codes`) THEN (SELECT `necropsyCodeId` FROM `necropsy_codes` WHERE `code` = `causeOfDeath` LIMIT 1)
                            WHEN `causeOfDeath` IN (SELECT `description` FROM `necropsy_codes`) THEN (SELECT `necropsyCodeId` FROM `necropsy_codes` WHERE `description` = `causeOfDeath` LIMIT 1)
                            ELSE 'N99'
                        END,
                        `responsibleWorker`, `notes`, `timestamp`, `record_guid`
                    FROM `mortalities`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `mortalities`")
                db.execSQL("ALTER TABLE `mortalities_new` RENAME TO `mortalities`")

                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_mortalities_animalId` ON `mortalities` (`animalId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mortalities_necropsy_code_id` ON `mortalities` (`necropsy_code_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_mortalities_record_guid` ON `mortalities` (`record_guid`)")

                // 7. Rebuild farmer_addresses table
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
                        `record_guid` TEXT NOT NULL,
                        PRIMARY KEY(`address_id`),
                        FOREIGN KEY(`farmer_id`) REFERENCES `farmers`(`farmer_id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`province`) REFERENCES `provinces`(`provinceId`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `farmer_addresses_new` (
                        `address_id`, `farmer_id`, `address_type`, `address_line_1`, `province`,
                        `postal_code`, `gps_latitude`, `gps_longitude`, `record_guid`
                    )
                    SELECT
                        `address_id`, `farmer_id`, `address_type`, `address_line_1`, `province`,
                        `postal_code`, `gps_latitude`, `gps_longitude`, `record_guid`
                    FROM `farmer_addresses`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `farmer_addresses`")
                db.execSQL("ALTER TABLE `farmer_addresses_new` RENAME TO `farmer_addresses`")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_farmer_addresses_farmer_id` ON `farmer_addresses` (`farmer_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_farmer_addresses_province` ON `farmer_addresses` (`province`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_farmer_addresses_record_guid` ON `farmer_addresses` (`record_guid`)")

                // 8. Rebuild animal_movements table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `animal_movements_new` (
                        `movement_id` TEXT NOT NULL,
                        `animal_id` TEXT NOT NULL,
                        `source_farm_id` TEXT,
                        `source_pen_id` TEXT,
                        `destination_farm_id` TEXT NOT NULL,
                        `destination_pen_id` TEXT NOT NULL,
                        `movement_date` INTEGER NOT NULL,
                        `feed_location_type` TEXT,
                        `notes` TEXT,
                        `record_guid` TEXT NOT NULL DEFAULT '',
                        `gps_lat` REAL NOT NULL DEFAULT 0.0,
                        `gps_lng` REAL NOT NULL DEFAULT 0.0,
                        `device_id` TEXT NOT NULL DEFAULT '',
                        `captured_at` INTEGER NOT NULL DEFAULT 0,
                        `sync_status` TEXT NOT NULL DEFAULT 'PENDING',
                        `synced_at` INTEGER,
                        PRIMARY KEY(`movement_id`),
                        FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`device_id`) REFERENCES `devices`(`deviceId`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `animal_movements_new` (
                        `movement_id`, `animal_id`, `source_farm_id`, `source_pen_id`,
                        `destination_farm_id`, `destination_pen_id`, `movement_date`,
                        `feed_location_type`, `notes`, `record_guid`, `gps_lat`, `gps_lng`,
                        `device_id`, `captured_at`, `sync_status`, `synced_at`
                    )
                    SELECT
                        `movement_id`, `animal_id`, `source_farm_id`, `source_pen_id`,
                        `destination_farm_id`, `destination_pen_id`, `movement_date`,
                        `feed_location_type`, `notes`, `record_guid`, `gps_lat`, `gps_lng`,
                        `device_id`, `captured_at`, `sync_status`, `synced_at`
                    FROM `animal_movements`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `animal_movements`")
                db.execSQL("ALTER TABLE `animal_movements_new` RENAME TO `animal_movements`")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_movements_animal_id` ON `animal_movements` (`animal_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_movements_destination_farm_id` ON `animal_movements` (`destination_farm_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_movements_destination_pen_id` ON `animal_movements` (`destination_pen_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_movements_device_id` ON `animal_movements` (`device_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_movements_record_guid` ON `animal_movements` (`record_guid`)")

                // 9. Rebuild animal_weights table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `animal_weights_new` (
                        `weight_id` TEXT NOT NULL,
                        `animal_id` TEXT NOT NULL,
                        `weight_kg` REAL NOT NULL,
                        `weigh_date` INTEGER NOT NULL,
                        `notes` TEXT,
                        `record_guid` TEXT NOT NULL DEFAULT '',
                        `gps_lat` REAL NOT NULL DEFAULT 0.0,
                        `gps_lng` REAL NOT NULL DEFAULT 0.0,
                        `device_id` TEXT NOT NULL DEFAULT '',
                        `captured_at` INTEGER NOT NULL DEFAULT 0,
                        `sync_status` TEXT NOT NULL DEFAULT 'PENDING',
                        `synced_at` INTEGER,
                        PRIMARY KEY(`weight_id`),
                        FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`device_id`) REFERENCES `devices`(`deviceId`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `animal_weights_new` (
                        `weight_id`, `animal_id`, `weight_kg`, `weigh_date`, `notes`,
                        `record_guid`, `gps_lat`, `gps_lng`, `device_id`, `captured_at`,
                        `sync_status`, `synced_at`
                    )
                    SELECT
                        `weight_id`, `animal_id`, `weight_kg`, `weigh_date`, `notes`,
                        `record_guid`, `gps_lat`, `gps_lng`, `device_id`, `captured_at`,
                        `sync_status`, `synced_at`
                    FROM `animal_weights`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `animal_weights`")
                db.execSQL("ALTER TABLE `animal_weights_new` RENAME TO `animal_weights`")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_weights_animal_id` ON `animal_weights` (`animal_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_weights_weigh_date` ON `animal_weights` (`weigh_date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_weights_device_id` ON `animal_weights` (`device_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_weights_record_guid` ON `animal_weights` (`record_guid`)")

                // 10. Create auto-lookup triggers
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS trg_animals_auto_lookup BEFORE INSERT ON animals BEGIN
                        INSERT INTO breeds (breedId, name)
                        SELECT NEW.breed, NEW.breed
                        WHERE NEW.breed IS NOT NULL AND NEW.breed != '' AND NOT EXISTS (SELECT 1 FROM breeds WHERE breedId = NEW.breed);

                        INSERT INTO hide_colours (colourId, name)
                        SELECT NEW.hideColour, NEW.hideColour
                        WHERE NEW.hideColour IS NOT NULL AND NEW.hideColour != '' AND NOT EXISTS (SELECT 1 FROM hide_colours WHERE colourId = NEW.hideColour);

                        INSERT INTO devices (deviceId)
                        SELECT NEW.deviceId
                        WHERE NEW.deviceId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM devices WHERE deviceId = NEW.deviceId);
                    END;
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS trg_treatments_auto_lookup BEFORE INSERT ON treatments BEGIN
                        INSERT INTO diseases (diseaseId, name)
                        SELECT NEW.disease, NEW.disease
                        WHERE NEW.disease IS NOT NULL AND NEW.disease != '' AND NOT EXISTS (SELECT 1 FROM diseases WHERE diseaseId = NEW.disease);

                        INSERT INTO devices (deviceId)
                        SELECT NEW.deviceId
                        WHERE NEW.deviceId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM devices WHERE deviceId = NEW.deviceId);
                    END;
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS trg_farmer_addresses_auto_lookup BEFORE INSERT ON farmer_addresses BEGIN
                        INSERT INTO provinces (provinceId, countryId, name)
                        SELECT NEW.province, 'ZAF', NEW.province
                        WHERE NEW.province IS NOT NULL AND NEW.province != '' AND NOT EXISTS (SELECT 1 FROM provinces WHERE provinceId = NEW.province);
                    END;
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS trg_movements_auto_device BEFORE INSERT ON animal_movements BEGIN
                        INSERT INTO devices (deviceId)
                        SELECT NEW.device_id
                        WHERE NEW.device_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM devices WHERE deviceId = NEW.device_id);
                    END;
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS trg_weights_auto_device BEFORE INSERT ON animal_weights BEGIN
                        INSERT INTO devices (deviceId)
                        SELECT NEW.device_id
                        WHERE NEW.device_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM devices WHERE deviceId = NEW.device_id);
                    END;
                    """.trimIndent()
                )

                // 11. Check FK integrity
                db.query("PRAGMA foreign_key_check").use { check(it.count == 0) { "FK violations after 23->24" } }
            }
        }

        /**
         * R4.1 Migration (Version 24 -> 25):
         * - Adds body_condition_score to animal_weights.
         * - Introduces identifier_types lookup table with FK from animal_identifiers.
         * - Adds triggers for active tag uniqueness and tag permanence.
         */
        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `animal_weights` ADD COLUMN `body_condition_score` TEXT")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `identifier_types` (
                        `code` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `validation_regex` TEXT,
                        PRIMARY KEY(`code`)
                    )
                    """.trimIndent()
                )

                IdentifierTypeSeed.execute(db)

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `animal_identifiers_new` (
                        `identifier_id` TEXT NOT NULL,
                        `animal_id` TEXT NOT NULL,
                        `identifier_type` TEXT NOT NULL,
                        `identifier_value` TEXT NOT NULL,
                        `valid_from` INTEGER,
                        `valid_to` INTEGER,
                        `record_guid` TEXT NOT NULL DEFAULT '',
                        PRIMARY KEY(`identifier_id`),
                        FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`identifier_type`) REFERENCES `identifier_types`(`code`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `animal_identifiers_new` (
                        `identifier_id`, `animal_id`, `identifier_type`, `identifier_value`, `valid_from`, `valid_to`, `record_guid`
                    )
                    SELECT `identifier_id`, `animal_id`, `identifier_type`, `identifier_value`, `valid_from`, `valid_to`, `record_guid`
                    FROM `animal_identifiers`
                    WHERE `identifier_type` IN (SELECT `code` FROM `identifier_types`)
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE `animal_identifiers`")
                db.execSQL("ALTER TABLE `animal_identifiers_new` RENAME TO `animal_identifiers`")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_identifiers_animal_id` ON `animal_identifiers` (`animal_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_identifiers_identifier_type_identifier_value` ON `animal_identifiers` (`identifier_type`, `identifier_value`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_identifiers_identifier_type` ON `animal_identifiers` (`identifier_type`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animal_identifiers_record_guid` ON `animal_identifiers` (`record_guid`)")

                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS trg_animal_identifiers_unique_active
                    BEFORE INSERT ON animal_identifiers
                    WHEN NEW.valid_to IS NULL
                    BEGIN
                        SELECT CASE WHEN EXISTS (
                            SELECT 1 FROM animal_identifiers
                            WHERE animal_id = NEW.animal_id
                              AND identifier_type = NEW.identifier_type
                              AND valid_to IS NULL
                        ) THEN RAISE(ABORT, 'Active identifier of this type already exists for animal')
                        END;
                    END;
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS trg_animal_identifiers_tag_permanence
                    BEFORE INSERT ON animal_identifiers
                    WHEN NEW.identifier_type = 'TAG'
                    BEGIN
                        SELECT CASE WHEN EXISTS (
                            SELECT 1 FROM animal_identifiers
                            WHERE identifier_type = 'TAG'
                              AND identifier_value = NEW.identifier_value
                              AND animal_id != NEW.animal_id
                        ) THEN RAISE(ABORT, 'TAG value has already been assigned to another animal')
                        END;
                    END;
                    """.trimIndent()
                )

                db.query("PRAGMA foreign_key_check").use { check(it.count == 0) { "FK violations after 24->25" } }
            }
        }

        /**
         * R4.2 Migration (Version 25 -> 26):
         * - Backfills data from legacy animals columns into animal_identifiers,
         *   animal_media, animal_weights, and animal_group_memberships.
         * - Adds single open group membership trigger.
         */
        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `animal_identifiers` (`identifier_id`, `animal_id`, `identifier_type`, `identifier_value`, `valid_from`, `record_guid`)
                    SELECT
                        lower(hex(randomblob(16))),
                        `animalId`,
                        'TAG',
                        `tagNumber`,
                        `captureAt`,
                        lower(hex(randomblob(16)))
                    FROM `animals`
                    WHERE `tagNumber` IS NOT NULL AND `tagNumber` != ''
                      AND NOT EXISTS (
                          SELECT 1 FROM `animal_identifiers` ai
                          WHERE ai.`animal_id` = `animals`.`animalId` AND ai.`identifier_type` = 'TAG'
                      )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `animal_identifiers` (`identifier_id`, `animal_id`, `identifier_type`, `identifier_value`, `valid_from`, `record_guid`)
                    SELECT
                        lower(hex(randomblob(16))),
                        `animalId`,
                        'OLD_TAG',
                        `oldTagNumber`,
                        `captureAt`,
                        lower(hex(randomblob(16)))
                    FROM `animals`
                    WHERE `oldTagNumber` IS NOT NULL AND `oldTagNumber` != ''
                      AND NOT EXISTS (
                          SELECT 1 FROM `animal_identifiers` ai
                          WHERE ai.`animal_id` = `animals`.`animalId` AND ai.`identifier_type` = 'OLD_TAG'
                      )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `animal_identifiers` (`identifier_id`, `animal_id`, `identifier_type`, `identifier_value`, `valid_from`, `record_guid`)
                    SELECT
                        lower(hex(randomblob(16))),
                        `animalId`,
                        'REFERENCE',
                        `referenceNumber`,
                        `captureAt`,
                        lower(hex(randomblob(16)))
                    FROM `animals`
                    WHERE `referenceNumber` IS NOT NULL AND `referenceNumber` != ''
                      AND NOT EXISTS (
                          SELECT 1 FROM `animal_identifiers` ai
                          WHERE ai.`animal_id` = `animals`.`animalId` AND ai.`identifier_type` = 'REFERENCE'
                      )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `animal_identifiers` (`identifier_id`, `animal_id`, `identifier_type`, `identifier_value`, `valid_from`, `record_guid`)
                    SELECT
                        lower(hex(randomblob(16))),
                        `animalId`,
                        'TEMPERATURE',
                        `temperatureNumber`,
                        `captureAt`,
                        lower(hex(randomblob(16)))
                    FROM `animals`
                    WHERE `temperatureNumber` IS NOT NULL AND `temperatureNumber` != ''
                      AND NOT EXISTS (
                          SELECT 1 FROM `animal_identifiers` ai
                          WHERE ai.`animal_id` = `animals`.`animalId` AND ai.`identifier_type` = 'TEMPERATURE'
                      )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `animal_media` (`media_id`, `animal_id`, `file_path`, `media_type`, `created_at`, `record_guid`)
                    SELECT
                        lower(hex(randomblob(16))),
                        `animalId`,
                        `photoPath`,
                        'PHOTO',
                        `captureAt`,
                        lower(hex(randomblob(16)))
                    FROM `animals`
                    WHERE `photoPath` IS NOT NULL AND `photoPath` != ''
                      AND NOT EXISTS (
                          SELECT 1 FROM `animal_media` am
                          WHERE am.`animal_id` = `animals`.`animalId` AND am.`media_type` = 'PHOTO' AND am.`file_path` = `animals`.`photoPath`
                      )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `animal_media` (`media_id`, `animal_id`, `file_path`, `media_type`, `created_at`, `record_guid`)
                    SELECT
                        lower(hex(randomblob(16))),
                        `animalId`,
                        `videoPath`,
                        'VIDEO',
                        `captureAt`,
                        lower(hex(randomblob(16)))
                    FROM `animals`
                    WHERE `videoPath` IS NOT NULL AND `videoPath` != ''
                      AND NOT EXISTS (
                          SELECT 1 FROM `animal_media` am
                          WHERE am.`animal_id` = `animals`.`animalId` AND am.`media_type` = 'VIDEO' AND am.`file_path` = `animals`.`videoPath`
                      )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `animal_weights` (
                        `weight_id`, `animal_id`, `weight_kg`, `weigh_date`, `body_condition_score`,
                        `record_guid`, `gps_lat`, `gps_lng`, `device_id`, `captured_at`, `sync_status`
                    )
                    SELECT
                        lower(hex(randomblob(16))),
                        `animalId`,
                        `massKg`,
                        `captureAt`,
                        `condition`,
                        lower(hex(randomblob(16))),
                        `gpsLat`,
                        `gpsLng`,
                        `deviceId`,
                        `captureAt`,
                        'PENDING'
                    FROM `animals`
                    WHERE `massKg` IS NOT NULL AND `massKg` > 0
                      AND NOT EXISTS (
                          SELECT 1 FROM `animal_weights` aw
                          WHERE aw.`animal_id` = `animals`.`animalId` AND aw.`weigh_date` = `animals`.`captureAt`
                      )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `animal_group_memberships` (
                        `membership_id`, `animal_id`, `group_id`, `joined_at`, `left_at`, `record_guid`
                    )
                    SELECT
                        lower(hex(randomblob(16))),
                        `animalId`,
                        `animalGroupId`,
                        `captureAt`,
                        NULL,
                        lower(hex(randomblob(16)))
                    FROM `animals`
                    WHERE `animalGroupId` IS NOT NULL AND `animalGroupId` != ''
                      AND `animalGroupId` IN (SELECT `animalGroupId` FROM `animal_groups`)
                      AND NOT EXISTS (
                          SELECT 1 FROM `animal_group_memberships` agm
                          WHERE agm.`animal_id` = `animals`.`animalId` AND agm.`group_id` = `animals`.`animalGroupId` AND agm.`left_at` IS NULL
                      )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS trg_animal_group_memberships_single_open
                    BEFORE INSERT ON animal_group_memberships
                    WHEN NEW.left_at IS NULL
                    BEGIN
                        SELECT CASE WHEN EXISTS (
                            SELECT 1 FROM animal_group_memberships
                            WHERE animal_id = NEW.animal_id
                              AND left_at IS NULL
                        ) THEN RAISE(ABORT, 'Animal already has an active group membership')
                        END;
                    END;
                    """.trimIndent()
                )

                db.query("PRAGMA foreign_key_check").use { check(it.count == 0) { "FK violations after 25->26" } }
            }
        }

        /**
         * R4.3 Migration (Version 26 -> 27):
         * - Replaces parentId with dam_id and sire_id foreign keys on animals.
         */
        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
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
                        `dam_id` TEXT,
                        `sire_id` TEXT,
                        `animalGroupId` TEXT,
                        `photoPath` TEXT,
                        `videoPath` TEXT,
                        `gpsLat` REAL NOT NULL,
                        `gpsLng` REAL NOT NULL,
                        `captureAt` INTEGER NOT NULL,
                        `deviceId` TEXT NOT NULL,
                        `record_guid` TEXT NOT NULL,
                        `syncStatus` TEXT NOT NULL,
                        `syncedat` INTEGER,
                        PRIMARY KEY(`animalId`),
                        FOREIGN KEY(`animalGroupId`) REFERENCES `animal_groups`(`animalGroupId`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`breed`) REFERENCES `breeds`(`breedId`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`hideColour`) REFERENCES `hide_colours`(`colourId`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`deviceId`) REFERENCES `devices`(`deviceId`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`dam_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`sire_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `animals_new` (
                        `animalId`, `tagNumber`, `oldTagNumber`, `temperatureNumber`, `referenceNumber`,
                        `massKg`, `birthdate`, `breed`, `gender`, `age`, `condition`, `hideColour`,
                        `brandMark`, `dam_id`, `sire_id`, `animalGroupId`, `photoPath`, `videoPath`,
                        `gpsLat`, `gpsLng`, `captureAt`, `deviceId`, `record_guid`, `syncStatus`, `syncedat`
                    )
                    SELECT
                        a.`animalId`, a.`tagNumber`, a.`oldTagNumber`, a.`temperatureNumber`, a.`referenceNumber`,
                        a.`massKg`, a.`birthdate`, a.`breed`, a.`gender`, a.`age`, a.`condition`, a.`hideColour`,
                        a.`brandMark`,
                        CASE
                            WHEN a.`parentId` IN (SELECT `animalId` FROM `animals`) THEN a.`parentId`
                            ELSE (SELECT ai.`animal_id` FROM `animal_identifiers` ai WHERE ai.`identifier_value` = a.`parentId` LIMIT 1)
                        END AS `dam_id`,
                        NULL AS `sire_id`,
                        a.`animalGroupId`, a.`photoPath`, a.`videoPath`,
                        a.`gpsLat`, a.`gpsLng`, a.`captureAt`, a.`deviceId`, a.`record_guid`, a.`syncStatus`, a.`syncedat`
                    FROM `animals` a
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE `animals`")
                db.execSQL("ALTER TABLE `animals_new` RENAME TO `animals`")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_tagNumber` ON `animals` (`tagNumber`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_temperatureNumber` ON `animals` (`temperatureNumber`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_dam_id` ON `animals` (`dam_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_sire_id` ON `animals` (`sire_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_animalGroupId` ON `animals` (`animalGroupId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_breed` ON `animals` (`breed`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_hideColour` ON `animals` (`hideColour`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_deviceId` ON `animals` (`deviceId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animals_record_guid` ON `animals` (`record_guid`)")

                db.query("PRAGMA foreign_key_check").use { check(it.count == 0) { "FK violations after 26->27" } }
            }
        }

        /**
         * R4.4 Migration (Version 27 -> 28):
         * - Drops deprecated legacy columns on animals.
         */
        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `animals_new` (
                        `animalId` TEXT NOT NULL,
                        `birthdate` INTEGER NOT NULL,
                        `breed` TEXT NOT NULL,
                        `gender` TEXT,
                        `hideColour` TEXT,
                        `brandMark` TEXT,
                        `dam_id` TEXT,
                        `sire_id` TEXT,
                        `gpsLat` REAL NOT NULL,
                        `gpsLng` REAL NOT NULL,
                        `captureAt` INTEGER NOT NULL,
                        `deviceId` TEXT NOT NULL,
                        `record_guid` TEXT NOT NULL,
                        `syncStatus` TEXT NOT NULL,
                        `syncedat` INTEGER,
                        PRIMARY KEY(`animalId`),
                        FOREIGN KEY(`breed`) REFERENCES `breeds`(`breedId`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`hideColour`) REFERENCES `hide_colours`(`colourId`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`deviceId`) REFERENCES `devices`(`deviceId`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`dam_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`sire_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `animals_new` (
                        `animalId`, `birthdate`, `breed`, `gender`, `hideColour`, `brandMark`,
                        `dam_id`, `sire_id`, `gpsLat`, `gpsLng`, `captureAt`, `deviceId`,
                        `record_guid`, `syncStatus`, `syncedat`
                    )
                    SELECT
                        `animalId`, `birthdate`, `breed`, `gender`, `hideColour`, `brandMark`,
                        `dam_id`, `sire_id`, `gpsLat`, `gpsLng`, `captureAt`, `deviceId`,
                        `record_guid`, `syncStatus`, `syncedat`
                    FROM `animals`
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE `animals`")
                db.execSQL("ALTER TABLE `animals_new` RENAME TO `animals`")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_dam_id` ON `animals` (`dam_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_sire_id` ON `animals` (`sire_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_breed` ON `animals` (`breed`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_hideColour` ON `animals` (`hideColour`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_animals_deviceId` ON `animals` (`deviceId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_animals_record_guid` ON `animals` (`record_guid`)")

                db.query("PRAGMA foreign_key_check").use { check(it.count == 0) { "FK violations after 27->28" } }
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
            private fun createLookupTriggers(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS trg_animals_auto_lookup BEFORE INSERT ON animals BEGIN
                        INSERT INTO breeds (breedId, name)
                        SELECT NEW.breed, NEW.breed
                        WHERE NEW.breed IS NOT NULL AND NEW.breed != '' AND NOT EXISTS (SELECT 1 FROM breeds WHERE breedId = NEW.breed);

                        INSERT INTO hide_colours (colourId, name)
                        SELECT NEW.hideColour, NEW.hideColour
                        WHERE NEW.hideColour IS NOT NULL AND NEW.hideColour != '' AND NOT EXISTS (SELECT 1 FROM hide_colours WHERE colourId = NEW.hideColour);

                        INSERT INTO devices (deviceId)
                        SELECT NEW.deviceId
                        WHERE NEW.deviceId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM devices WHERE deviceId = NEW.deviceId);
                    END;
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS trg_treatments_auto_lookup BEFORE INSERT ON treatments BEGIN
                        INSERT INTO diseases (diseaseId, name)
                        SELECT NEW.disease, NEW.disease
                        WHERE NEW.disease IS NOT NULL AND NEW.disease != '' AND NOT EXISTS (SELECT 1 FROM diseases WHERE diseaseId = NEW.disease);

                        INSERT INTO devices (deviceId)
                        SELECT NEW.deviceId
                        WHERE NEW.deviceId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM devices WHERE deviceId = NEW.deviceId);
                    END;
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS trg_farmer_addresses_auto_lookup BEFORE INSERT ON farmer_addresses BEGIN
                        INSERT INTO provinces (provinceId, countryId, name)
                        SELECT NEW.province, 'ZAF', NEW.province
                        WHERE NEW.province IS NOT NULL AND NEW.province != '' AND NOT EXISTS (SELECT 1 FROM provinces WHERE provinceId = NEW.province);
                    END;
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS trg_movements_auto_device BEFORE INSERT ON animal_movements BEGIN
                        INSERT INTO devices (deviceId)
                        SELECT NEW.device_id
                        WHERE NEW.device_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM devices WHERE deviceId = NEW.device_id);
                    END;
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS trg_weights_auto_device BEFORE INSERT ON animal_weights BEGIN
                        INSERT INTO devices (deviceId)
                        SELECT NEW.device_id
                        WHERE NEW.device_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM devices WHERE deviceId = NEW.device_id);
                    END;
                    """.trimIndent()
                )
            }

            override fun onCreate(db: SupportSQLiteDatabase) {
                CostTypeSeed.execute(db)
                RoleSeed.execute(db)
                BreedSeed.execute(db)
                HideColourSeed.execute(db)
                DiseaseSeed.execute(db)
                MedicationSeed.execute(db)
                CountryProvinceSeed.execute(db)
                NecropsyCodeSeed.execute(db)
                IdentifierTypeSeed.execute(db)
                createLookupTriggers(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                CostTypeSeed.execute(db)
                RoleSeed.execute(db)
                BreedSeed.execute(db)
                HideColourSeed.execute(db)
                DiseaseSeed.execute(db)
                MedicationSeed.execute(db)
                CountryProvinceSeed.execute(db)
                NecropsyCodeSeed.execute(db)
                IdentifierTypeSeed.execute(db)
                createLookupTriggers(db)
            }
        }
    }
}
