package com.beeftech.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * R0.7: a v10 database (858548c^, the last commit before the buggy 10->11
 * migration shipped) migrated all the way to the latest version, with no
 * destructive fallback, must keep every original row and end up with a
 * unique, non-empty GUID everywhere one is required.
 *
 * There is no exported schema JSON for v10-v13 (only 14, 16 and 17 exist),
 * so the starting database is written by hand here, using the same v10
 * table shapes the still-present (but unregistered) AnimalMovement,
 * CalfRegistration, Supplier and LocationFeed entity classes describe, and
 * migrations are chained directly rather than through MigrationTestHelper.
 */
@RunWith(AndroidJUnit4::class)
class Migration10To18Test {

    private lateinit var context: Context
    private val databaseName = "migration_10_to_18_test.db"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrate10To18_keepsEveryRow_andEveryGuidIsUniqueAndNonBlank() {
        val helperFactory = FrameworkSQLiteOpenHelperFactory()

        val animalAId = UUID.randomUUID().toString()
        val animalBId = UUID.randomUUID().toString()
        val movementNoTagRecordGuid = "movement-guid-1"

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(object : SupportSQLiteOpenHelper.Callback(10) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // --- v10 tables untouched by this migration range ---
                    db.execSQL(
                        "CREATE TABLE `animals` (`animalId` TEXT NOT NULL, `tagNumber` TEXT, `oldTagNumber` TEXT, " +
                                "`temperatureNumber` TEXT, `referenceNumber` TEXT, `massKg` REAL, `birthdate` INTEGER NOT NULL, " +
                                "`breed` TEXT NOT NULL, `gender` TEXT, `age` INTEGER, `condition` TEXT, `hideColour` TEXT, " +
                                "`brandMark` TEXT, `parentId` TEXT, `animalGroupId` TEXT, `photoPath` TEXT, `videoPath` TEXT, " +
                                "`gpsLat` REAL NOT NULL, `gpsLng` REAL NOT NULL, `captureAt` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, " +
                                "`recordguid` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, `syncedat` INTEGER, PRIMARY KEY(`animalId`))"
                    )
                    db.execSQL(
                        "CREATE TABLE `animal_identifiers` (`identifier_id` TEXT NOT NULL, `animal_id` TEXT NOT NULL, " +
                                "`identifier_type` TEXT NOT NULL, `identifier_value` TEXT NOT NULL, `valid_from` TEXT, `valid_to` TEXT, " +
                                "PRIMARY KEY(`identifier_id`))"
                    )
                    db.execSQL(
                        "CREATE TABLE `animal_media` (`media_id` TEXT NOT NULL, `animal_id` TEXT NOT NULL, `file_path` TEXT NOT NULL, " +
                                "`media_type` TEXT NOT NULL, `created_at` TEXT NOT NULL, PRIMARY KEY(`media_id`))"
                    )
                    db.execSQL(
                        "CREATE TABLE `farmers` (`farmer_id` TEXT NOT NULL PRIMARY KEY, `client_code` TEXT, `organisation_name` TEXT, " +
                                "`vat_number` TEXT, `email_address` TEXT, `gps_latitude` REAL, `gps_longitude` REAL, `sync_status` TEXT NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE `farmer_roles` (`farmer_role_id` TEXT NOT NULL PRIMARY KEY, `farmer_id` TEXT NOT NULL, `role_id` TEXT NOT NULL)"
                    )
                    // Empty, on purpose: nothing seeded `roles` before #47 (N1).
                    db.execSQL("CREATE TABLE `roles` (`role_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `role_name` TEXT NOT NULL)")

                    // --- v10 tables D2/D3 rewrote the 10->11 handling of ---
                    db.execSQL(
                        "CREATE TABLE `treatments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `animalId` TEXT NOT NULL, " +
                                "`disease` TEXT NOT NULL, `treatmentName` TEXT NOT NULL, `batchNumber` TEXT NOT NULL, " +
                                "`volumeUsed` TEXT NOT NULL, `cost` REAL NOT NULL, `timestamp` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE `animal_movements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `animalId` TEXT NOT NULL, " +
                                "`movementType` TEXT NOT NULL, `responsibleWorker` TEXT NOT NULL DEFAULT '', `timestamp` INTEGER NOT NULL, " +
                                "`gpsLat` REAL NOT NULL DEFAULT 0.0, `gpsLng` REAL NOT NULL DEFAULT 0.0, `deviceId` TEXT NOT NULL DEFAULT '', " +
                                "`recordguid` TEXT NOT NULL DEFAULT '', `syncStatus` TEXT NOT NULL DEFAULT 'PENDING', `syncedAt` INTEGER)"
                    )
                    db.execSQL(
                        "CREATE TABLE `calf_registrations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `animalId` TEXT NOT NULL, " +
                                "`birthdate` INTEGER NOT NULL, `breed` TEXT NOT NULL, `damId` TEXT, `sireId` TEXT, `photoPath` TEXT, " +
                                "`videoPath` TEXT, `gpsLat` REAL NOT NULL, `gpsLng` REAL NOT NULL, `captureAt` INTEGER NOT NULL, " +
                                "`deviceId` TEXT NOT NULL, `recordguid` TEXT NOT NULL DEFAULT '', `syncStatus` TEXT NOT NULL DEFAULT 'PENDING', " +
                                "`syncedat` INTEGER)"
                    )
                    db.execSQL(
                        "CREATE TABLE `suppliers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `animalId` TEXT NOT NULL, " +
                                "`supplierName` TEXT NOT NULL, `glnNumber` TEXT NOT NULL, `purchaseDate` TEXT NOT NULL, " +
                                "`purchaseBatchNumber` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE `location_feed` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `animalId` TEXT NOT NULL, " +
                                "`destination` TEXT NOT NULL, `daysInDestination` INTEGER NOT NULL, `rationName` TEXT NOT NULL, " +
                                "`rationDays` INTEGER NOT NULL, `rationCost` REAL NOT NULL, `timestamp` INTEGER NOT NULL)"
                    )

                    // --- seed data ---
                    db.execSQL(
                        "INSERT INTO `animals` (`animalId`, `tagNumber`, `birthdate`, `breed`, `gpsLat`, `gpsLng`, `captureAt`, `deviceId`, `recordguid`, `syncStatus`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        arrayOf<Any>(animalAId, "Blu0000001", 1_600_000_000_000, "Bonsmara", 0.0, 0.0, 1_600_000_000_000, "device-1", UUID.randomUUID().toString(), "SYNCED")
                    )
                    db.execSQL(
                        "INSERT INTO `animals` (`animalId`, `tagNumber`, `birthdate`, `breed`, `gpsLat`, `gpsLng`, `captureAt`, `deviceId`, `recordguid`, `syncStatus`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        arrayOf<Any>(animalBId, "Red0000002", 1_600_000_000_000, "Angus", 0.0, 0.0, 1_600_000_000_000, "device-1", UUID.randomUUID().toString(), "SYNCED")
                    )

                    // 3 treatments, all with recordguid = '' (the column doesn't
                    // exist yet in the real v10 table -- 10->11 introduces it).
                    // 2+ blank recordguids is exactly what used to crash the
                    // upgrade (D3) when the UNIQUE index was created too early.
                    listOf(1, 2, 3).forEach { n ->
                        db.execSQL(
                            "INSERT INTO `treatments` (`animalId`, `disease`, `treatmentName`, `batchNumber`, `volumeUsed`, `cost`, `timestamp`) VALUES (?, ?, ?, ?, ?, ?, ?)",
                            arrayOf<Any>(animalAId, "Disease$n", "Treatment$n", "BATCH$n", "10ml", 20.0 * n, 1_600_000_000_000L + n)
                        )
                    }

                    // 2 movements: one already keyed on the UUID with a real
                    // recordguid, one keyed on a tag (pre-D1 device) with a
                    // blank recordguid.
                    db.execSQL(
                        "INSERT INTO `animal_movements` (`animalId`, `movementType`, `responsibleWorker`, `timestamp`, `recordguid`) VALUES (?, ?, ?, ?, ?)",
                        arrayOf<Any>(animalAId, "Feedlot B", "Jan", 1_600_000_100_000L, movementNoTagRecordGuid)
                    )
                    db.execSQL(
                        "INSERT INTO `animal_movements` (`animalId`, `movementType`, `responsibleWorker`, `timestamp`, `recordguid`) VALUES (?, ?, ?, ?, ?)",
                        arrayOf<Any>("Red0000002", "Feedlot C", "Piet", 1_600_000_200_000L, "")
                    )

                    // 2 calf registrations: one brand-new tag (blank recordguid,
                    // so registration_id must fall back to a fresh UUID), one
                    // with a dam and sire that both resolve via tagNumber, a
                    // photo, and a real recordguid that must become the
                    // registration_id.
                    db.execSQL(
                        "INSERT INTO `calf_registrations` (`animalId`, `birthdate`, `breed`, `gpsLat`, `gpsLng`, `captureAt`, `deviceId`, `recordguid`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        arrayOf<Any>("Blu0000010", 1_700_000_000_000L, "Bonsmara", 0.0, 0.0, 1_700_000_000_000L, "device-1", "")
                    )
                    db.execSQL(
                        "INSERT INTO `calf_registrations` (`animalId`, `birthdate`, `breed`, `damId`, `sireId`, `photoPath`, `gpsLat`, `gpsLng`, `captureAt`, `deviceId`, `recordguid`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        arrayOf<Any>("Red0000020", 1_700_000_100_000L, "Angus", "Blu0000001", "Red0000002", "/photos/calf2.jpg", 0.0, 0.0, 1_700_000_100_000L, "device-1", "reg-guid-2")
                    )

                    db.execSQL(
                        "INSERT INTO `suppliers` (`animalId`, `supplierName`, `glnNumber`, `purchaseDate`, `purchaseBatchNumber`, `timestamp`) VALUES (?, ?, ?, ?, ?, ?)",
                        arrayOf<Any>(animalAId, "ACME Feeds", "GLN-001", "2025-01-01", "BATCH-S1", 1_600_000_300_000L)
                    )
                    db.execSQL(
                        "INSERT INTO `location_feed` (`animalId`, `destination`, `daysInDestination`, `rationName`, `rationDays`, `rationCost`, `timestamp`) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        arrayOf<Any>(animalAId, "Pen 4", 10, "Grower Mix", 10, 150.0, 1_600_000_400_000L)
                    )

                    db.execSQL(
                        "INSERT INTO `farmers` (`farmer_id`, `sync_status`) VALUES ('FARMER-1', 'PENDING')"
                    )
                    db.execSQL(
                        "INSERT INTO `farmer_roles` (`farmer_role_id`, `farmer_id`, `role_id`) VALUES ('FR-1', 'FARMER-1', '2')"
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    BeefTechDatabase.MIGRATION_10_11.migrate(db)
                    BeefTechDatabase.MIGRATION_11_12.migrate(db)
                    BeefTechDatabase.MIGRATION_12_13.migrate(db)
                    BeefTechDatabase.MIGRATION_13_14.migrate(db)
                    BeefTechDatabase.MIGRATION_14_16.migrate(db)
                    BeefTechDatabase.MIGRATION_16_17.migrate(db)
                    BeefTechDatabase.MIGRATION_17_18.migrate(db)
                }
            })
            .build()

        // Create at v10.
        helperFactory.create(config).writableDatabase.close()

        // Reopen at v18 to trigger onUpgrade.
        val migratedConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(object : SupportSQLiteOpenHelper.Callback(18) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    BeefTechDatabase.MIGRATION_10_11.migrate(db)
                    BeefTechDatabase.MIGRATION_11_12.migrate(db)
                    BeefTechDatabase.MIGRATION_12_13.migrate(db)
                    BeefTechDatabase.MIGRATION_13_14.migrate(db)
                    BeefTechDatabase.MIGRATION_14_16.migrate(db)
                    BeefTechDatabase.MIGRATION_16_17.migrate(db)
                    BeefTechDatabase.MIGRATION_17_18.migrate(db)
                }
            })
            .build()
        val db = helperFactory.create(migratedConfig).writableDatabase

        // --- every original row is present, either in its target table or a legacy_* table ---
        db.query("SELECT COUNT(*) FROM `legacy_animal_movements`").use { c -> c.moveToFirst(); assertEquals(2, c.getInt(0)) }
        db.query("SELECT COUNT(*) FROM `legacy_calf_registrations`").use { c -> c.moveToFirst(); assertEquals(2, c.getInt(0)) }
        db.query("SELECT COUNT(*) FROM `legacy_suppliers`").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        db.query("SELECT COUNT(*) FROM `location_feed`").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        db.query("SELECT COUNT(*) FROM `treatments`").use { c -> c.moveToFirst(); assertEquals(3, c.getInt(0)) }

        // --- both movements were recovered into the real table, tag re-keyed to the UUID ---
        db.query("SELECT `animal_id`, `destination_farm_id`, `notes` FROM `animal_movements` WHERE `movement_id` = ?", arrayOf<Any>(movementNoTagRecordGuid)).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(animalAId, c.getString(0))
            assertEquals("Feedlot B", c.getString(1))
            assertEquals("Jan", c.getString(2))
        }
        db.query("SELECT `animal_id` FROM `animal_movements` WHERE `destination_farm_id` = 'Feedlot C'").use { c ->
            assertTrue("The tag-keyed movement must be recovered, re-keyed to the animal's UUID", c.moveToFirst())
            assertEquals(animalBId, c.getString(0))
        }

        // --- both calves were recovered, dam/sire resolved by tag ---
        var calf1AnimalId: String? = null
        db.query(
            "SELECT a.`animalId`, cr.`registration_id` FROM `calf_registrations` cr " +
                    "JOIN `animals` a ON a.`animalId` = cr.`registered_animal_id` " +
                    "JOIN `animal_identifiers` i ON i.`animal_id` = a.`animalId` " +
                    "WHERE i.`identifier_type` = 'TAG' AND i.`identifier_value` = 'Blu0000010'"
        ).use { c ->
            assertTrue("The new-tag calf must have created its own animal + identifier row", c.moveToFirst())
            calf1AnimalId = c.getString(0)
            assertTrue("A blank recordguid must fall back to a generated registration_id", c.getString(1).isNotBlank())
        }

        db.query(
            "SELECT cr.`registration_id`, cr.`dam_id`, cr.`sire_id` FROM `calf_registrations` cr WHERE cr.`registration_id` = 'reg-guid-2'"
        ).use { c ->
            assertTrue("A non-blank recordguid must be kept as the registration_id", c.moveToFirst())
            assertEquals(animalAId, c.getString(1))
            assertEquals(animalBId, c.getString(2))
        }

        db.query("SELECT COUNT(*) FROM `animal_media` WHERE `animal_id` != ? AND `animal_id` != ?", arrayOf<Any>(animalAId, animalBId)).use { c ->
            c.moveToFirst()
            assertEquals("The photo on the recovered calf must produce an animal_media row", 1, c.getInt(0))
        }

        // --- farmer roles survive, and roles ended up seeded ---
        db.query("SELECT COUNT(*) FROM `roles`").use { c -> c.moveToFirst(); assertEquals(3, c.getInt(0)) }
        db.query("SELECT `role_id` FROM `farmer_roles` WHERE `farmer_role_id` = 'FR-1'").use { c ->
            assertTrue("Farmer roles must survive even though `roles` started empty", c.moveToFirst())
            assertEquals(2L, c.getLong(0))
        }

        // --- every GUID that must be unique is unique and non-blank ---
        db.query("SELECT `recordguid` FROM `treatments`").use { c ->
            val guids = mutableSetOf<String>()
            while (c.moveToNext()) {
                val guid = c.getString(0)
                assertFalse("Every treatment recordguid must be non-blank after the full chain", guid.isNullOrBlank())
                guids += guid
            }
            assertEquals("Every treatment recordguid must be unique", 3, guids.size)
        }
        db.query("SELECT `recordguid` FROM `animals`").use { c ->
            var count = 0
            val guids = mutableSetOf<String>()
            while (c.moveToNext()) {
                count++
                val guid = c.getString(0)
                assertFalse("Every animal recordguid must be non-blank", guid.isNullOrBlank())
                guids += guid
            }
            assertEquals("Every animal recordguid must be unique", count, guids.size)
        }
        db.query("SELECT `record_guid` FROM `calf_registrations`").use { c ->
            var count = 0
            val guids = mutableSetOf<String>()
            while (c.moveToNext()) {
                count++
                val guid = c.getString(0)
                assertFalse(guid.isNullOrBlank())
                guids += guid
            }
            assertEquals(2, count)
            assertEquals(2, guids.size)
        }

        // --- no fallback wiped anything; the FK graph is consistent end to end ---
        db.query("PRAGMA foreign_key_check").use { c ->
            assertEquals("No FK violations should remain after the full 10->18 chain", 0, c.count)
        }

        db.close()
    }
}
