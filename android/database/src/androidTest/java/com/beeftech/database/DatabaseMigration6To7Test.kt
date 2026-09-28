package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.Animal
import com.beeftech.database.entity.AnimalCost
import com.beeftech.database.entity.AnimalMovementEntity
import com.beeftech.database.entity.Mortality
import com.beeftech.database.entity.Treatment
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigration6To7Test {

    private lateinit var context: Context

    private val databaseName =
        "beeftech.db"

    private fun newPassphrase(): ByteArray {

        return "migration-test-passphrase"
            .toByteArray(
                Charsets.UTF_8
            )
    }

    @Before
    fun setup() {

        context =
            ApplicationProvider
                .getApplicationContext()

        /*
         * Make sure no previous database
         * connection is still open.
         */
        DatabaseProvider.close()

        /*
         * Start with a completely clean
         * encrypted database.
         */
        context.deleteDatabase(
            databaseName
        )
    }

    @After
    fun tearDown() {

        DatabaseProvider.close()

        context.deleteDatabase(
            databaseName
        )
    }

    @Test
    fun migrateVersion6To7_preservesExistingData_andCreatesAnimalCosts() =
        runBlocking {

            /*
             * ------------------------------------------------
             * STEP 1
             *
             * Create the current encrypted database first.
             * This gives us the exact Room schema for all
             * existing tables.
             * ------------------------------------------------
             */

            val initialResult =
                DatabaseFactory.create(
                    context =
                        context,

                    passphrase =
                        newPassphrase()
                )

            assertTrue(
                initialResult
                        is DatabaseResult.Success
            )

            val initialDatabase =
                when (initialResult) {

                    is DatabaseResult.Success ->
                        initialResult.database

                    is DatabaseResult.Error ->
                        throw AssertionError(
                            "Unable to create initial database: " +
                                    initialResult.message
                        )
                }

            /*
             * ------------------------------------------------
             * STEP 2
             *
             * Insert records that represent data already
             * stored before the version 7 upgrade.
             * ------------------------------------------------
             */

            /*
             * animal_movements.animal_id is a foreign key
             * to animals, so the animal must exist first.
             */
            initialDatabase
                .animalDao()
                .insert(
                    Animal(
                        animalId =
                            "MIG-001",

                        birthdate =
                            1000L,

                        breed =
                            "Bonsmara",

                        gpsLat =
                            -26.2041,

                        gpsLng =
                            28.0473,

                        captureAt =
                            1000L,

                        deviceId =
                            "migration-test"
                    )
                )

            initialDatabase
                .animalMovementDao()
                .insert(
                    AnimalMovementEntity(
                        animalId =
                            "MIG-001",

                        destinationFarmId =
                            "Moved to Feedlot A",

                        destinationPenId =
                            "",

                        movementDate =
                            "1000",

                        notes =
                            "Migration Worker"
                    )
                )

            initialDatabase
                .treatmentDao()
                .insert(
                    Treatment(
                        animalId =
                            "MIG-001",

                        disease =
                            "Respiratory infection",

                        treatmentName =
                            "Antibiotic",

                        batchNumber =
                            "MIG-BATCH-001",

                        volumeUsed =
                            "10 ml",

                        cost =
                            150.0,

                        gpsLat =
                            -26.2041,

                        gpsLng =
                            28.0473,

                        timestamp =
                            2000L
                    )
                )

            initialDatabase
                .mortalityDao()
                .insert(
                    Mortality(
                        animalId =
                            "MIG-001",

                        causeOfDeath =
                            "Test migration record",

                        responsibleWorker =
                            "Migration Worker",

                        notes =
                            "Migration test",

                        timestamp =
                            3000L
                    )
                )

            /*
             * Confirm database is currently at the latest version.
             */
            assertEquals(
                20,
                initialDatabase
                    .openHelper
                    .writableDatabase
                    .version
            )

            /*
             * ------------------------------------------------
             * STEP 3
             *
             * Simulate the VERSION 6 schema.
             *
             * Version 7 only added animal_costs, so remove
             * that table and change user_version back to 6.
             *
             * All the version 6 tables and data remain.
             * ------------------------------------------------
             */

            val rawDatabase =
                initialDatabase
                    .openHelper
                    .writableDatabase

            rawDatabase.execSQL(
                "DROP TABLE animal_costs"
            )

            /*
             * R0.2 made MIGRATION_10_11 read `animal_movements` by its real
             * v10 column names instead of dropping the table unread, so a
             * "v6" animal_movements must actually have the v10 shape too --
             * not the v18 one `initialDatabase` just created it with -- or
             * that migration fails with "no such column: animalId".
             */
            rawDatabase.execSQL(
                "DROP TABLE animal_movements"
            )
            rawDatabase.execSQL(
                """
                CREATE TABLE animal_movements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    animalId TEXT NOT NULL,
                    movementType TEXT NOT NULL,
                    responsibleWorker TEXT NOT NULL DEFAULT '',
                    timestamp INTEGER NOT NULL,
                    gpsLat REAL NOT NULL DEFAULT 0.0,
                    gpsLng REAL NOT NULL DEFAULT 0.0,
                    deviceId TEXT NOT NULL DEFAULT '',
                    recordguid TEXT NOT NULL DEFAULT '',
                    syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                    syncedAt INTEGER
                )
                """.trimIndent()
            )
            rawDatabase.execSQL(
                "INSERT INTO animal_movements (animalId, movementType, responsibleWorker, timestamp) VALUES (?, ?, ?, ?)",
                arrayOf<Any>("MIG-001", "Moved to Feedlot A", "Migration Worker", 1000L)
            )

            // Same reason: `calf_registrations` and `suppliers` must exist in
            // their real v10 shape (even empty) or MIGRATION_10_11's legacy
            // copy / recovery queries fail with "no such column"/"no such
            // table" -- the v18 schema this test started from either has a
            // different shape (calf_registrations) or doesn't have the table
            // at all anymore (suppliers).
            rawDatabase.execSQL("DROP TABLE calf_registrations")
            rawDatabase.execSQL(
                """
                CREATE TABLE calf_registrations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    animalId TEXT NOT NULL,
                    birthdate INTEGER NOT NULL,
                    breed TEXT NOT NULL,
                    damId TEXT,
                    sireId TEXT,
                    photoPath TEXT,
                    videoPath TEXT,
                    gpsLat REAL NOT NULL,
                    gpsLng REAL NOT NULL,
                    captureAt INTEGER NOT NULL,
                    deviceId TEXT NOT NULL,
                    recordguid TEXT NOT NULL DEFAULT '',
                    syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                    syncedat INTEGER
                )
                """.trimIndent()
            )
            rawDatabase.execSQL(
                """
                CREATE TABLE suppliers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    animalId TEXT NOT NULL,
                    supplierName TEXT NOT NULL,
                    glnNumber TEXT NOT NULL,
                    purchaseDate TEXT NOT NULL,
                    purchaseBatchNumber TEXT NOT NULL,
                    timestamp INTEGER NOT NULL
                )
                """.trimIndent()
            )

            /*
             * R3 renamed `recordguid` to `record_guid` on `animals` and
             * `treatments` (bullet 1). `initialDatabase` just created both
             * tables in that new shape, so a "v6" copy needs the old column
             * name back, or an earlier migration that still reads/writes
             * `recordguid` directly (e.g. MIGRATION_13_14's derived-cost
             * backfill, which selects `treatments.recordguid`) fails with
             * "no such column: recordguid". MIGRATION_18_19 rebuilds both
             * tables from scratch, so the stale index left behind by the
             * rename doesn't matter -- it's dropped along with the table.
             */
            rawDatabase.execSQL("ALTER TABLE animals RENAME COLUMN record_guid TO recordguid")
            rawDatabase.execSQL("ALTER TABLE treatments RENAME COLUMN record_guid TO recordguid")

            /*
             * R3 bullet 2 added `record_guid` to nine more tables in
             * MIGRATION_19_20, via `ALTER TABLE ... ADD COLUMN`. These eight
             * are untouched by 6->7 and hold no test data, so they only need
             * their pre-R3 (v19) shape back -- without it, MIGRATION_19_20's
             * `ADD COLUMN record_guid` fails with "duplicate column name"
             * against the column `initialDatabase` already created fresh.
             * (`animal_movements` is excluded: it's already rebuilt above in
             * its v10 shape, which predates `record_guid` too.)
             */
            rawDatabase.execSQL("DROP TABLE `animal_weights`")
            rawDatabase.execSQL("CREATE TABLE IF NOT EXISTS `animal_weights` (`weight_id` TEXT NOT NULL, `animal_id` TEXT NOT NULL, `weight_kg` REAL NOT NULL, `weigh_date` TEXT NOT NULL, `notes` TEXT, PRIMARY KEY(`weight_id`), FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            rawDatabase.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_weights_animal_id` ON `animal_weights` (`animal_id`)")
            rawDatabase.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_weights_weigh_date` ON `animal_weights` (`weigh_date`)")

            rawDatabase.execSQL("DROP TABLE `animal_identifiers`")
            rawDatabase.execSQL("CREATE TABLE IF NOT EXISTS `animal_identifiers` (`identifier_id` TEXT NOT NULL, `animal_id` TEXT NOT NULL, `identifier_type` TEXT NOT NULL, `identifier_value` TEXT NOT NULL, `valid_from` TEXT, `valid_to` TEXT, PRIMARY KEY(`identifier_id`), FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            rawDatabase.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_identifiers_animal_id` ON `animal_identifiers` (`animal_id`)")
            rawDatabase.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_identifiers_identifier_type_identifier_value` ON `animal_identifiers` (`identifier_type`, `identifier_value`)")

            rawDatabase.execSQL("DROP TABLE `animal_media`")
            rawDatabase.execSQL("CREATE TABLE IF NOT EXISTS `animal_media` (`media_id` TEXT NOT NULL, `animal_id` TEXT NOT NULL, `file_path` TEXT NOT NULL, `media_type` TEXT NOT NULL, `created_at` TEXT NOT NULL, PRIMARY KEY(`media_id`), FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            rawDatabase.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_media_animal_id` ON `animal_media` (`animal_id`)")

            rawDatabase.execSQL("DROP TABLE `animal_ownerships`")
            rawDatabase.execSQL("CREATE TABLE IF NOT EXISTS `animal_ownerships` (`ownership_id` TEXT NOT NULL, `animal_id` TEXT NOT NULL, `owner_name` TEXT NOT NULL, `ownership_percentage` REAL NOT NULL, `start_date` TEXT NOT NULL, `end_date` TEXT, PRIMARY KEY(`ownership_id`), FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            rawDatabase.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_ownerships_animal_id` ON `animal_ownerships` (`animal_id`)")
            rawDatabase.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_ownerships_owner_name` ON `animal_ownerships` (`owner_name`)")
            rawDatabase.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_ownerships_start_date` ON `animal_ownerships` (`start_date`)")

            rawDatabase.execSQL("DROP TABLE `animal_purchases`")
            rawDatabase.execSQL("CREATE TABLE IF NOT EXISTS `animal_purchases` (`purchase_id` TEXT NOT NULL, `animal_id` TEXT NOT NULL, `purchase_price` REAL NOT NULL, `purchase_date` TEXT NOT NULL, `seller_name` TEXT NOT NULL, `notes` TEXT, PRIMARY KEY(`purchase_id`), FOREIGN KEY(`animal_id`) REFERENCES `animals`(`animalId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            rawDatabase.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_purchases_animal_id` ON `animal_purchases` (`animal_id`)")
            rawDatabase.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_purchases_purchase_date` ON `animal_purchases` (`purchase_date`)")

            rawDatabase.execSQL("DROP TABLE `farmer_addresses`")
            rawDatabase.execSQL("CREATE TABLE IF NOT EXISTS `farmer_addresses` (`address_id` TEXT NOT NULL, `farmer_id` TEXT NOT NULL, `address_type` TEXT, `address_line_1` TEXT, `province` TEXT, `postal_code` TEXT, `gps_latitude` REAL, `gps_longitude` REAL, PRIMARY KEY(`address_id`), FOREIGN KEY(`farmer_id`) REFERENCES `farmers`(`farmer_id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            rawDatabase.execSQL("CREATE INDEX IF NOT EXISTS `index_farmer_addresses_farmer_id` ON `farmer_addresses` (`farmer_id`)")

            rawDatabase.execSQL("DROP TABLE `feed_crib_readings`")
            rawDatabase.execSQL("CREATE TABLE IF NOT EXISTS `feed_crib_readings` (`id` TEXT NOT NULL, `cribId` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`cribId`) REFERENCES `feed_cribs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            rawDatabase.execSQL("CREATE INDEX IF NOT EXISTS `index_feed_crib_readings_cribId` ON `feed_crib_readings` (`cribId`)")

            rawDatabase.execSQL("DROP TABLE `feed_crib_reading_values`")
            rawDatabase.execSQL("CREATE TABLE IF NOT EXISTS `feed_crib_reading_values` (`id` TEXT NOT NULL, `readingId` TEXT NOT NULL, `value` REAL NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`readingId`) REFERENCES `feed_crib_readings`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            rawDatabase.execSQL("CREATE INDEX IF NOT EXISTS `index_feed_crib_reading_values_readingId` ON `feed_crib_reading_values` (`readingId`)")

            rawDatabase.execSQL(
                "PRAGMA user_version = 6"
            )

            assertEquals(
                6,
                rawDatabase.version
            )

            /*
             * Close the simulated old database.
             */
            initialDatabase.close()

            /*
             * ------------------------------------------------
             * STEP 4
             *
             * Open it again using the REAL production
             * DatabaseFactory.
             *
             * Room should detect version 6 and execute
             * MIGRATION_6_7 followed by MIGRATION_7_8.
             * ------------------------------------------------
             */

            val migrationResult =
                DatabaseFactory.create(
                    context =
                        context,

                    passphrase =
                        newPassphrase()
                )

            val upgradedDatabase =
                when (migrationResult) {

                    is DatabaseResult.Success ->
                        migrationResult.database

                    is DatabaseResult.Error ->
                        throw AssertionError(
                            "Migration 6 -> 7 failed: " +
                                    migrationResult.message
                        )
                }

            /*
             * Database must now be at the latest version.
             */
            assertEquals(
                20,
                upgradedDatabase
                    .openHelper
                    .writableDatabase
                    .version
            )

            /*
             * ------------------------------------------------
             * STEP 5
             *
             * Confirm OLD records were preserved.
             * ------------------------------------------------
             */

            val movements =
                upgradedDatabase
                    .animalMovementDao()
                    .getByAnimalId(
                        "MIG-001"
                    )

            assertEquals(
                1,
                movements.size
            )

            assertEquals(
                "Moved to Feedlot A",
                movements.first().movementType
            )

            val treatments =
                upgradedDatabase
                    .treatmentDao()
                    .getByAnimalId(
                        "MIG-001"
                    )

            assertEquals(
                1,
                treatments.size
            )

            assertEquals(
                150.0,
                treatments.first().cost,
                0.001
            )

            val mortalities =
                upgradedDatabase
                    .mortalityDao()
                    .getByAnimalId(
                        "MIG-001"
                    )

            assertEquals(
                1,
                mortalities.size
            )

            assertEquals(
                "Migration Worker",
                mortalities
                    .first()
                    .responsibleWorker
            )

            /*
             * ------------------------------------------------
             * STEP 6
             *
             * Confirm MIGRATION_6_7 created the new
             * animal_costs table successfully.
             * ------------------------------------------------
             */

            val existingTransportTotal =
                upgradedDatabase
                    .animalCostDao()
                    .getTotalByType(
                        animalId =
                            "MIG-001",

                        costType =
                            "TRANSPORT"
                    )

            /*
             * No AnimalCost existed in v6,
             * therefore the initial value should be zero.
             */
            assertEquals(
                0.0,
                existingTransportTotal,
                0.001
            )

            /*
             * Now prove the new table actually works.
             */
            upgradedDatabase
                .animalCostDao()
                .insert(
                    AnimalCost(
                        animalId =
                            "MIG-001",

                        costType =
                            "TRANSPORT",

                        amount =
                            250.0,

                        description =
                            "Transport after migration",

                        gpsLat =
                            -26.2041,

                        gpsLng =
                            28.0473,

                        timestamp =
                            6000L
                    )
                )

            val transportAfterInsert =
                upgradedDatabase
                    .animalCostDao()
                    .getTotalByType(
                        animalId =
                            "MIG-001",

                        costType =
                            "TRANSPORT"
                    )

            assertEquals(
                250.0,
                transportAfterInsert,
                0.001
            )

            upgradedDatabase.close()
        }
}

