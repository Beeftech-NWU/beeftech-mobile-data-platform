package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
             * Confirm database is currently v8.
             */
            assertEquals(
                8,
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

            assertTrue(
                migrationResult
                        is DatabaseResult.Success
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
             * Database must now be version 8.
             */
            assertEquals(
                8,
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

