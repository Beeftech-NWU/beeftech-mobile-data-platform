package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.dao.SyncSecurityDao
import com.beeftech.database.entity.FarmerEntity
import com.beeftech.database.entity.Treatment
import com.beeftech.database.entity.IdentifierTypes
import com.beeftech.database.entity.CalfRegistrationEntity
import com.beeftech.database.entity.AnimalMovementEntity
import com.beeftech.database.entity.Mortality
import com.beeftech.database.entity.AnimalCost
import com.beeftech.database.entity.CostType
import com.beeftech.database.entity.AnimalMediaEntity
import com.beeftech.database.entity.AnimalIdentifierEntity
import com.beeftech.database.entity.Animal
import com.beeftech.database.entity.PendingSync
import com.beeftech.database.repository.SyncPolicyEnforcer
import com.beeftech.database.repository.SyncPolicyStore
import com.beeftech.database.repository.SyncWarningPolicy
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CompletableDeferred
import com.beeftech.database.repository.SyncRepository
import com.beeftech.database.security.CurrentUserIdRegistry
import com.beeftech.database.repository.PendingSyncRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncPolicyEnforcerTest {

    private lateinit var context: Context
    private lateinit var database: BeefTechDatabase
    private lateinit var enforcer: SyncPolicyEnforcer

    @Before
    fun setUp() {

        context =
            ApplicationProvider
                .getApplicationContext()

        context.deleteDatabase(
            DATABASE_NAME
        )

        val result =
            DatabaseFactory.create(
                context =
                    context,

                passphrase =
                    createPassphrase()
            )

        assertTrue(
            "Database must open successfully.",
            result is DatabaseResult.Success
        )

        database =
            (result as DatabaseResult.Success)
                .database

        enforcer =
            SyncPolicyEnforcer(
                pendingSyncDao =
                    database.pendingSyncDao(),

                syncSecurityDao =
                    database.syncSecurityDao()
            )
    }

    @After
    fun tearDown() {

        CurrentUserIdRegistry.clear()

        if (
            ::database.isInitialized
        ) {
            database.close()
        }

        context.deleteDatabase(
            DATABASE_NAME
        )
    }


    // ========================================================
    // DAY 2
    // ========================================================

    @Test
    fun day2_createsWarningWithoutWipingOrLocking() =
        runBlocking {

            insertPendingFarmer(
                farmerId =
                    "DAY2-FARMER",

                ageDays =
                    2
            )

            val result =
                enforcer.evaluate(
                    userId =
                        USER_ID,

                    now =
                        NOW
                )

            assertEquals(
                1,
                result.warningLevel
            )

            assertFalse(
                result.accountLocked
            )

            assertEquals(
                1,
                database
                    .pendingSyncDao()
                    .getPendingCount()
            )

            assertNotNull(
                database
                    .farmerDao()
                    .getFarmerById(
                        "DAY2-FARMER"
                    )
            )

            assertEquals(
                setOf(2),
                warningDays()
            )
        }


    // ========================================================
    // DAY 4
    // ========================================================

    @Test
    fun day4_createsExpectedWarningsWithoutWipingOrLocking() =
        runBlocking {

            insertPendingFarmer(
                farmerId =
                    "DAY4-FARMER",

                ageDays =
                    4
            )

            val result =
                enforcer.evaluate(
                    userId =
                        USER_ID,

                    now =
                        NOW
                )

            assertEquals(
                2,
                result.warningLevel
            )

            assertFalse(
                result.accountLocked
            )

            assertEquals(
                1,
                database
                    .pendingSyncDao()
                    .getPendingCount()
            )

            assertNotNull(
                database
                    .farmerDao()
                    .getFarmerById(
                        "DAY4-FARMER"
                    )
            )

            assertEquals(
                setOf(
                    2,
                    4
                ),
                warningDays()
            )
        }


    // ========================================================
    // DAY 6
    // ========================================================

    @Test
    fun day6_createsFinalWarningWithoutWipingOrLocking() =
        runBlocking {

            insertPendingFarmer(
                farmerId =
                    "DAY6-FARMER",

                ageDays =
                    6
            )

            val result =
                enforcer.evaluate(
                    userId =
                        USER_ID,

                    now =
                        NOW
                )

            assertEquals(
                3,
                result.warningLevel
            )

            assertFalse(
                result.accountLocked
            )

            assertEquals(
                1,
                database
                    .pendingSyncDao()
                    .getPendingCount()
            )

            assertNotNull(
                database
                    .farmerDao()
                    .getFarmerById(
                        "DAY6-FARMER"
                    )
            )

            assertEquals(
                setOf(
                    2,
                    4,
                    6
                ),
                warningDays()
            )
        }


    // ========================================================
    // DAY 7
    // ========================================================

    @Test
    fun day7_wipesOnlyUnsyncedDataAndLocksAccount() =
        runBlocking {

            /*
             * This farmer represents local data that never
             * synchronized and must therefore be removed.
             */
            database
                .farmerDao()
                .insertFarmer(
                    testFarmer(
                        farmerId =
                            "UNSYNCED-FARMER",

                        syncStatus =
                            "PENDING"
                    )
                )

            /*
             * This represents server-confirmed data.
             *
             * We deliberately put a stale queue item against it
             * so this test proves that Day 7 never deletes a
             * record whose local state is already SYNCED.
             */
            database
                .farmerDao()
                .insertFarmer(
                    testFarmer(
                        farmerId =
                            "SYNCED-FARMER",

                        syncStatus =
                            "SYNCED"
                    )
                )

            val createdAt =
                NOW -
                        (
                            7L *
                                    DAY_MS
                            )

            database
                .pendingSyncDao()
                .insert(
                    PendingSync(
                        userId = USER_ID,

                        entityType =
                            SyncSecurityDao
                                .ENTITY_FARMER_REGISTRATION,

                        entityId =
                            "UNSYNCED-FARMER",

                        operation =
                            "CREATE",

                        payload =
                            "UNSYNCED-FARMER",

                        createdAt =
                            createdAt
                    )
                )

            database
                .pendingSyncDao()
                .insert(
                    PendingSync(
                        userId = USER_ID,

                        entityType =
                            SyncSecurityDao
                                .ENTITY_FARMER_REGISTRATION,

                        entityId =
                            "SYNCED-FARMER",

                        operation =
                            "CREATE",

                        payload =
                            "SYNCED-FARMER",

                        createdAt =
                            createdAt
                    )
                )

            val result =
                enforcer.evaluate(
                    userId =
                        USER_ID,

                    now =
                        NOW
                )

            assertEquals(
                4,
                result.warningLevel
            )

            assertTrue(
                result.accountLocked
            )

            /*
             * The unsynced local farmer must be gone.
             */
            assertNull(
                database
                    .farmerDao()
                    .getFarmerById(
                        "UNSYNCED-FARMER"
                    )
            )

            /*
             * CRITICAL SAFETY ASSERTION:
             *
             * Synced data must survive Day 7.
             */
            val syncedFarmer =
                database
                    .farmerDao()
                    .getFarmerById(
                        "SYNCED-FARMER"
                    )

            assertNotNull(
                syncedFarmer
            )

            assertEquals(
                "SYNCED",
                syncedFarmer
                    ?.sync_status
            )

            /*
             * Day-7 queue is cleared only after processing.
             */
            assertEquals(
                0,
                database
                    .pendingSyncDao()
                    .getPendingCount()
            )

            /*
             * Account lock must persist in Room.
             */
            assertTrue(
                database
                    .syncSecurityDao()
                    .isLocked(
                        USER_ID
                    )
            )

            val events =
                database
                    .syncSecurityDao()
                    .getAllEvents()

            val warningDays =
                events
                    .filter {
                        it.eventType ==
                                SyncSecurityDao.EVENT_WARNING
                    }
                    .mapNotNull {
                        it.warningDay
                    }
                    .toSet()

            assertEquals(
                setOf(
                    2,
                    4,
                    6
                ),
                warningDays
            )

            val eventTypes =
                events
                    .map {
                        it.eventType
                    }
                    .toSet()

            assertTrue(
                SyncSecurityDao.EVENT_DAY_7_TRIGGERED
                        in eventTypes
            )

            assertTrue(
                SyncSecurityDao.EVENT_DAY_7_WIPE
                        in eventTypes
            )

            assertTrue(
                SyncSecurityDao.EVENT_ACCOUNT_LOCKED
                        in eventTypes
            )

            /*
             * Run the policy again.
             *
             * It must remain locked without creating another
             * destructive Day-7 action.
             */
            val eventCountBefore =
                events.size

            val secondResult =
                enforcer.evaluate(
                    userId =
                        USER_ID,

                    now =
                        NOW +
                                DAY_MS
                )

            assertTrue(
                secondResult.accountLocked
            )

            assertEquals(
                eventCountBefore,
                database
                    .syncSecurityDao()
                    .getAllEvents()
                    .size
            )
        }



    // ========================================================
    // DAY 7 - CALF REGISTRATION
    // ========================================================

    @Test
    fun day7_wipesUnsyncedCalfRegistrationGraph() =
        runBlocking {

            val animalId =
                "DAY7-CALF-ANIMAL"

            val recordGuid =
                "DAY7-CALF-GUID"

            database
                .calfRegistrationDao()
                .registerCalf(
                    animal =
                        testAnimal(
                            animalId =
                                animalId,

                            syncStatus =
                                "PENDING"
                        ),

                    identifiers =
                        listOf(
                            AnimalIdentifierEntity(
                                identifierId =
                                    "DAY7-CALF-TAG-ID",

                                animalId =
                                    animalId,

                                identifierType =
                                    IdentifierTypes.TAG,

                                identifierValue =
                                    "DAY7-CALF-TAG",

                                validFrom =
                                    NOW,

                                validTo =
                                    null,

                                recordGuid =
                                    "DAY7-CALF-TAG-GUID"
                            )
                        ),

                    media =
                        listOf(
                            AnimalMediaEntity(
                                mediaId =
                                    "DAY7-CALF-MEDIA",

                                animalId =
                                    animalId,

                                filePath =
                                    "/tmp/day7-calf.jpg",

                                mediaType =
                                    "PHOTO",

                                createdAt =
                                    NOW,

                                recordGuid =
                                    "DAY7-CALF-MEDIA-GUID"
                            )
                        ),

                    registration =
                        CalfRegistrationEntity(
                            registrationId =
                                "DAY7-CALF-REG",

                            registeredAnimalId =
                                animalId,

                            registrationDate =
                                NOW,

                            recordGuid =
                                recordGuid,

                            syncStatus =
                                "PENDING",

                            syncedAt =
                                null
                        )
                )

            insertDay7Queue(
                entityType =
                    SyncSecurityDao
                        .ENTITY_CALF_REGISTRATION,

                entityId =
                    recordGuid
            )

            val result =
                enforcer.evaluate(
                    userId =
                        USER_ID,

                    now =
                        NOW
                )

            assertTrue(
                result.accountLocked
            )

            /*
             * Main calf animal must be gone.
             */
            assertNull(
                database
                    .animalDao()
                    .getById(
                        animalId
                    )
            )

            /*
             * The FK graph must also disappear:
             * calf registration, tag and media.
             */
            assertEquals(
                0,
                countRows(
                    "SELECT COUNT(*) " +
                            "FROM calf_registrations " +
                            "WHERE record_guid = '$recordGuid'"
                )
            )

            assertEquals(
                0,
                countRows(
                    "SELECT COUNT(*) " +
                            "FROM animal_identifiers " +
                            "WHERE animal_id = '$animalId'"
                )
            )

            assertEquals(
                0,
                countRows(
                    "SELECT COUNT(*) " +
                            "FROM animal_media " +
                            "WHERE animal_id = '$animalId'"
                )
            )

            assertEquals(
                0,
                database
                    .pendingSyncDao()
                    .getPendingCount()
            )
        }


    // ========================================================
    // DAY 7 - MOVEMENT
    // ========================================================

    @Test
    fun day7_wipesUnsyncedMovementButPreservesAnimal() =
        runBlocking {

            val animalId =
                "DAY7-MOVEMENT-ANIMAL"

            val recordGuid =
                "DAY7-MOVEMENT-GUID"

            database
                .animalDao()
                .insert(
                    testAnimal(
                        animalId =
                            animalId,

                        syncStatus =
                            "SYNCED"
                    )
                )

            database
                .animalMovementDao()
                .insert(
                    AnimalMovementEntity(
                        movementId =
                            "DAY7-MOVEMENT-ID",

                        animalId =
                            animalId,

                        destinationFarmId =
                            "DAY7-FARM",

                        destinationPenId =
                            "DAY7-PEN",

                        movementDate =
                            NOW,

                        notes =
                            "Day 7 safety test",

                        recordGuid =
                            recordGuid,

                        deviceId =
                            "DAY7-DEVICE",

                        capturedAt =
                            NOW,

                        syncStatus =
                            "PENDING",

                        syncedAt =
                            null
                    )
                )

            insertDay7Queue(
                entityType =
                    SyncSecurityDao
                        .ENTITY_ANIMAL_MOVEMENT,

                entityId =
                    recordGuid
            )

            val result =
                enforcer.evaluate(
                    userId =
                        USER_ID,

                    now =
                        NOW
                )

            assertTrue(
                result.accountLocked
            )

            /*
             * Unsynced movement is removed.
             */
            assertNull(
                database
                    .animalMovementDao()
                    .findByRecordGuid(
                        recordGuid
                    )
            )

            /*
             * The animal itself is server-confirmed data
             * and must remain.
             */
            assertNotNull(
                database
                    .animalDao()
                    .getById(
                        animalId
                    )
            )

            assertEquals(
                0,
                database
                    .pendingSyncDao()
                    .getPendingCount()
            )
        }


    // ========================================================
    // DAY 7 - MORTALITY
    // ========================================================

    @Test
    fun day7_wipesUnsyncedMortalityButPreservesAnimal() =
        runBlocking {

            val animalId =
                "DAY7-MORTALITY-ANIMAL"

            val recordGuid =
                "DAY7-MORTALITY-GUID"

            database
                .animalDao()
                .insert(
                    testAnimal(
                        animalId =
                            animalId,

                        syncStatus =
                            "SYNCED"
                    )
                )

            database
                .mortalityDao()
                .insert(
                    Mortality(
                        animalId =
                            animalId,

                        causeOfDeath =
                            "Day 7 safety test",

                        timestamp =
                            NOW,

                        recordGuid =
                            recordGuid
                    )
                )

            insertDay7Queue(
                entityType =
                    SyncSecurityDao
                        .ENTITY_MORTALITY,

                entityId =
                    recordGuid
            )

            val result =
                enforcer.evaluate(
                    userId =
                        USER_ID,

                    now =
                        NOW
                )

            assertTrue(
                result.accountLocked
            )

            assertNull(
                database
                    .mortalityDao()
                    .findByRecordGuid(
                        recordGuid
                    )
            )

            assertNotNull(
                database
                    .animalDao()
                    .getById(
                        animalId
                    )
            )

            assertEquals(
                0,
                database
                    .pendingSyncDao()
                    .getPendingCount()
            )
        }

    @Test
    fun day7_keepsSyncedMortalityWithLeftoverQueueItem() =
        runBlocking {

            val animalId =
                "DAY7-SYNCED-MORTALITY-ANIMAL"

            val recordGuid =
                "DAY7-SYNCED-MORTALITY-GUID"

            database
                .animalDao()
                .insert(
                    testAnimal(
                        animalId =
                            animalId,

                        syncStatus =
                            "SYNCED"
                    )
                )

            database
                .mortalityDao()
                .insert(
                    Mortality(
                        animalId =
                            animalId,

                        causeOfDeath =
                            "Already on the server",

                        timestamp =
                            NOW,

                        recordGuid =
                            recordGuid,

                        syncStatus =
                            "SYNCED",

                        syncedAt =
                            NOW
                    )
                )

            insertDay7Queue(
                entityType =
                    SyncSecurityDao
                        .ENTITY_MORTALITY,

                entityId =
                    recordGuid
            )

            enforcer.evaluate(
                userId =
                    USER_ID,

                now =
                    NOW
            )

            /*
             * The server already has this mortality, so the wipe
             * must not delete the local copy.
             */
            assertNotNull(
                database
                    .mortalityDao()
                    .findByRecordGuid(
                        recordGuid
                    )
            )
        }


    // ========================================================
    // DAY 7 - ANIMAL COST
    // ========================================================

    private suspend fun insertCostWithParents(
        animalId: String,
        recordGuid: String,
        syncStatus: String
    ) {

        database
            .animalDao()
            .insert(
                testAnimal(
                    animalId =
                        animalId,

                    syncStatus =
                        "SYNCED"
                )
            )

        database
            .costTypeDao()
            .insertAll(
                listOf(
                    CostType(
                        code = "TRANSPORT",
                        displayName = "Transport",
                        sortOrder = 1
                    )
                )
            )

        database
            .animalCostDao()
            .insert(
                AnimalCost(
                    animalId =
                        animalId,

                    costType =
                        "TRANSPORT",

                    amount =
                        100.0,

                    gpsLat =
                        0.0,

                    gpsLng =
                        0.0,

                    timestamp =
                        NOW,

                    recordGuid =
                        recordGuid,

                    syncStatus =
                        syncStatus
                )
            )
    }

    @Test
    fun day7_wipesUnsyncedCostButPreservesAnimal() =
        runBlocking {

            val animalId =
                "DAY7-COST-ANIMAL"

            val recordGuid =
                "DAY7-COST-GUID"

            insertCostWithParents(
                animalId = animalId,
                recordGuid = recordGuid,
                syncStatus = "PENDING"
            )

            insertDay7Queue(
                entityType =
                    SyncSecurityDao
                        .ENTITY_ANIMAL_COST,

                entityId =
                    recordGuid
            )

            val result =
                enforcer.evaluate(
                    userId =
                        USER_ID,

                    now =
                        NOW
                )

            assertTrue(
                result.accountLocked
            )

            assertNull(
                database
                    .animalCostDao()
                    .findByRecordGuid(
                        recordGuid
                    )
            )

            assertNotNull(
                database
                    .animalDao()
                    .getById(
                        animalId
                    )
            )

            assertEquals(
                0,
                database
                    .pendingSyncDao()
                    .getPendingCount()
            )
        }

    @Test
    fun day7_keepsSyncedCostWithLeftoverQueueItem() =
        runBlocking {

            val animalId =
                "DAY7-SYNCED-COST-ANIMAL"

            val recordGuid =
                "DAY7-SYNCED-COST-GUID"

            insertCostWithParents(
                animalId = animalId,
                recordGuid = recordGuid,
                syncStatus = "SYNCED"
            )

            insertDay7Queue(
                entityType =
                    SyncSecurityDao
                        .ENTITY_ANIMAL_COST,

                entityId =
                    recordGuid
            )

            enforcer.evaluate(
                userId =
                    USER_ID,

                now =
                    NOW
            )

            /*
             * The server already has this cost, so the wipe
             * must not delete the local copy.
             */
            assertNotNull(
                database
                    .animalCostDao()
                    .findByRecordGuid(
                        recordGuid
                    )
            )
        }


    // ========================================================
    // DAY 7 - TREATMENT + DERIVED COST
    // ========================================================

    @Test
    fun day7_wipesTreatmentAndDerivedCostButPreservesAnimal() =
        runBlocking {

            val animalId =
                "DAY7-TREATMENT-ANIMAL"

            val recordGuid =
                "DAY7-TREATMENT-GUID"

            database
                .animalDao()
                .insert(
                    testAnimal(
                        animalId =
                            animalId,

                        syncStatus =
                            "SYNCED"
                    )
                )

            database
                .treatmentDao()
                .insertWithCost(
                    Treatment(
                        animalId =
                            animalId,

                        disease =
                            "DAY7-DISEASE",

                        treatmentName =
                            "Day 7 Treatment",

                        batchNumber =
                            "DAY7-BATCH",

                        volumeUsed =
                            "10 ml",

                        cost =
                            125.0,

                        gpsLat =
                            0.0,

                        gpsLng =
                            0.0,

                        timestamp =
                            NOW,

                        deviceId =
                            "DAY7-DEVICE",

                        recordGuid =
                            recordGuid,

                        syncStatus =
                            "PENDING",

                        syncedAt =
                            null
                    )
                )

            /*
             * Confirm the derived cost exists BEFORE enforcement.
             */
            assertEquals(
                1,
                countRows(
                    "SELECT COUNT(*) " +
                            "FROM animal_costs " +
                            "WHERE source_entity = 'TREATMENT' " +
                            "AND source_record_id = '$recordGuid'"
                )
            )

            insertDay7Queue(
                entityType =
                    SyncSecurityDao
                        .ENTITY_TREATMENT,

                entityId =
                    recordGuid
            )

            val result =
                enforcer.evaluate(
                    userId =
                        USER_ID,

                    now =
                        NOW
                )

            assertTrue(
                result.accountLocked
            )

            /*
             * Treatment must be deleted.
             */
            assertNull(
                database
                    .treatmentDao()
                    .findByRecordGuid(
                        recordGuid
                    )
            )

            /*
             * Its derived cost must also disappear.
             */
            assertEquals(
                0,
                countRows(
                    "SELECT COUNT(*) " +
                            "FROM animal_costs " +
                            "WHERE source_entity = 'TREATMENT' " +
                            "AND source_record_id = '$recordGuid'"
                )
            )

            /*
             * Treatment deletion must NOT delete the animal.
             */
            assertNotNull(
                database
                    .animalDao()
                    .getById(
                        animalId
                    )
            )

            assertEquals(
                0,
                database
                    .pendingSyncDao()
                    .getPendingCount()
            )
        }



    // ========================================================
    // DAY 7 - USER OWNERSHIP ISOLATION
    // ========================================================

    @Test
    fun day7_onlyProcessesRecordsOwnedByEvaluatedUser() =
        runBlocking {

            /*
             * USER-A owns this record.
             * It is old enough for Day 7.
             */
            database
                .farmerDao()
                .insertFarmer(
                    testFarmer(
                        farmerId =
                            "USER-A-FARMER",

                        syncStatus =
                            "PENDING"
                    )
                )

            /*
             * USER-B has a separate pending record on the
             * same physical device.
             */
            database
                .farmerDao()
                .insertFarmer(
                    testFarmer(
                        farmerId =
                            "USER-B-FARMER",

                        syncStatus =
                            "PENDING"
                    )
                )

            /*
             * Simulates a record that existed before v31.
             * Ownership is deliberately unknown.
             */
            database
                .farmerDao()
                .insertFarmer(
                    testFarmer(
                        farmerId =
                            "LEGACY-FARMER",

                        syncStatus =
                            "PENDING"
                    )
                )


            val createdAt =
                NOW -
                        (
                            7L *
                                    DAY_MS
                            )


            // USER-A
            database
                .pendingSyncDao()
                .insert(
                    PendingSync(
                        userId =
                            USER_ID,

                        entityType =
                            SyncSecurityDao
                                .ENTITY_FARMER_REGISTRATION,

                        entityId =
                            "USER-A-FARMER",

                        operation =
                            "CREATE",

                        payload =
                            "USER-A-FARMER",

                        createdAt =
                            createdAt
                    )
                )


            // USER-B
            database
                .pendingSyncDao()
                .insert(
                    PendingSync(
                        userId =
                            SECOND_USER_ID,

                        entityType =
                            SyncSecurityDao
                                .ENTITY_FARMER_REGISTRATION,

                        entityId =
                            "USER-B-FARMER",

                        operation =
                            "CREATE",

                        payload =
                            "USER-B-FARMER",

                        createdAt =
                            createdAt
                    )
                )


            // Legacy / unknown owner
            database
                .pendingSyncDao()
                .insert(
                    PendingSync(
                        userId =
                            null,

                        entityType =
                            SyncSecurityDao
                                .ENTITY_FARMER_REGISTRATION,

                        entityId =
                            "LEGACY-FARMER",

                        operation =
                            "CREATE",

                        payload =
                            "LEGACY-FARMER",

                        createdAt =
                            createdAt
                    )
                )


            /*
             * Evaluate USER-A only.
             */
            val result =
                enforcer.evaluate(
                    userId =
                        USER_ID,

                    now =
                        NOW
                )


            assertTrue(
                result.accountLocked
            )


            /*
             * USER-A record must be removed.
             */
            assertNull(
                database
                    .farmerDao()
                    .getFarmerById(
                        "USER-A-FARMER"
                    )
            )


            /*
             * USER-B must remain completely untouched.
             */
            assertNotNull(
                database
                    .farmerDao()
                    .getFarmerById(
                        "USER-B-FARMER"
                    )
            )


            /*
             * Legacy unknown-owner data must also remain.
             */
            assertNotNull(
                database
                    .farmerDao()
                    .getFarmerById(
                        "LEGACY-FARMER"
                    )
            )


            /*
             * USER-A queue is now empty.
             */
            assertEquals(
                0,
                database
                    .pendingSyncDao()
                    .getPendingCountForUser(
                        USER_ID
                    )
            )


            /*
             * USER-B queue remains pending.
             */
            assertEquals(
                1,
                database
                    .pendingSyncDao()
                    .getPendingCountForUser(
                        SECOND_USER_ID
                    )
            )


            /*
             * Legacy NULL queue remains untouched.
             */
            val allRemaining =
                database
                    .pendingSyncDao()
                    .getAll()

            assertEquals(
                1,
                allRemaining.count {
                    it.userId == null
                }
            )


            /*
             * Lock USER-A only.
             */
            assertTrue(
                database
                    .syncSecurityDao()
                    .isLocked(
                        USER_ID
                    )
            )

            assertFalse(
                database
                    .syncSecurityDao()
                    .isLocked(
                        SECOND_USER_ID
                    )
            )


            /*
             * Audit entries from this evaluation must belong
             * only to USER-A.
             */
            val events =
                database
                    .syncSecurityDao()
                    .getAllEvents()

            assertTrue(
                events.isNotEmpty()
            )

            assertTrue(
                events.all {
                    it.userId ==
                            USER_ID
                }
            )
        }



    // ========================================================
    // LIVE QUEUE OWNERSHIP
    // ========================================================

    @Test
    fun pendingRepository_assignsRegisteredCurrentUser() =
        runBlocking {

            CurrentUserIdRegistry.register {
                USER_ID
            }

            val repository =
                PendingSyncRepository(
                    database.pendingSyncDao()
                )

            repository.queueOperation(
                entityType =
                    SyncSecurityDao
                        .ENTITY_FARMER_REGISTRATION,

                entityId =
                    "OWNERSHIP-TEST",

                operation =
                    "CREATE",

                payload =
                    "OWNERSHIP-TEST"
            )

            val owned =
                database
                    .pendingSyncDao()
                    .getAllForUser(
                        USER_ID
                    )

            assertEquals(
                1,
                owned.size
            )

            assertEquals(
                USER_ID,
                owned.single().userId
            )
        }



    // ========================================================
    // REACTIVE USER-SCOPED WARNING DATA
    // ========================================================

    @Test
    fun syncRepository_pendingCountFollowsActiveUser() =
        runBlocking {

            /*
             * USER-A has one queued operation.
             */
            database
                .pendingSyncDao()
                .insert(
                    PendingSync(
                        userId =
                            USER_ID,

                        entityType =
                            SyncSecurityDao
                                .ENTITY_FARMER_REGISTRATION,

                        entityId =
                            "A-ONE",

                        operation =
                            "CREATE",

                        payload =
                            "A-ONE",

                        createdAt =
                            NOW
                    )
                )


            /*
             * USER-B has two queued operations.
             */
            repeat(2) {
                    index ->

                database
                    .pendingSyncDao()
                    .insert(
                        PendingSync(
                            userId =
                                SECOND_USER_ID,

                            entityType =
                                SyncSecurityDao
                                    .ENTITY_FARMER_REGISTRATION,

                            entityId =
                                "B-$index",

                            operation =
                                "CREATE",

                            payload =
                                "B-$index",

                            createdAt =
                                NOW
                        )
                    )
            }


            /*
             * Begin as USER-A.
             */
            CurrentUserIdRegistry
                .setCurrentUserId(
                    USER_ID
                )


            val repository =
                SyncRepository(
                    pendingSyncDao =
                        database.pendingSyncDao(),

                    syncBatchDao =
                        database.syncBatchDao()
                )


            val observedCounts =
                mutableListOf<Int>()


            /*
             * This signal is completed only after Room has actually
             * delivered USER-A's count.
             *
             * Do not use yield() here. yield() only gives another
             * coroutine an opportunity to run; it does not guarantee
             * that the asynchronous Room query has emitted.
             */
            val firstUserObserved =
                CompletableDeferred<Unit>()


            val collection =
                launch {

                    repository
                        .observePendingCount()
                        .take(2)
                        .collect {
                                count ->

                            observedCounts +=
                                count


                            if (
                                observedCounts.size ==
                                1
                            ) {

                                firstUserObserved
                                    .complete(
                                        Unit
                                    )
                            }
                        }
                }


            /*
             * Wait until USER-A's count has definitely reached the
             * collector before changing authenticated accounts.
             *
             * The timeout converts any future regression into a clear
             * test failure instead of an indefinitely hanging test.
             */
            withTimeout(
                5_000L
            ) {

                firstUserObserved
                    .await()
            }


            assertEquals(
                1,
                observedCounts.first()
            )


            /*
             * Switch accounts without recreating the repository.
             *
             * flatMapLatest should abandon USER-A's Room query and
             * subscribe to USER-B's user-scoped query.
             */
            CurrentUserIdRegistry
                .setCurrentUserId(
                    SECOND_USER_ID
                )


            withTimeout(
                5_000L
            ) {

                collection
                    .join()
            }


            assertEquals(
                listOf(
                    1,
                    2
                ),
                observedCounts
            )
        }


    // ========================================================
    // Test helpers
    // ========================================================

    private suspend fun insertDay7Queue(
        entityType: String,
        entityId: String
    ) {

        database
            .pendingSyncDao()
            .insert(
                PendingSync(
                    userId = USER_ID,

                    entityType =
                        entityType,

                    entityId =
                        entityId,

                    operation =
                        "CREATE",

                    payload =
                        entityId,

                    createdAt =
                        NOW -
                                (
                                    7L *
                                            DAY_MS
                                    )
                )
            )
    }

    private fun testAnimal(
        animalId: String,
        syncStatus: String
    ): Animal {

        return Animal(
            animalId =
                animalId,

            birthdate =
                NOW,

            breed =
                "DAY7-TEST-BREED",

            gender =
                "FEMALE",

            gpsLat =
                0.0,

            gpsLng =
                0.0,

            captureAt =
                NOW,

            deviceId =
                "DAY7-DEVICE",

            recordGuid =
                "$animalId-GUID",

            syncStatus =
                syncStatus,

            syncedat =
                if (
                    syncStatus ==
                        "SYNCED"
                ) {
                    NOW
                } else {
                    null
                }
        )
    }

    private fun countRows(
        sql: String
    ): Int {

        val cursor =
            database
                .openHelper
                .writableDatabase
                .query(
                    sql
                )

        cursor.use {

            if (
                !it.moveToFirst()
            ) {
                return 0
            }

            return it.getInt(0)
        }
    }

    // ========================================================
    // CONFIGURABLE WARNING DAYS (the wipe is not configurable)
    // ========================================================

    private fun enforcerWith(
        policy: suspend () -> SyncWarningPolicy
    ) = SyncPolicyEnforcer(
        pendingSyncDao = database.pendingSyncDao(),
        syncSecurityDao = database.syncSecurityDao(),
        policyProvider = policy
    )

    @Test
    fun customWarningDays_raiseTheLevelsOnTheirOwnDays() =
        runBlocking {

            val custom = enforcerWith { SyncWarningPolicy.sanitize(listOf(1, 3, 5)) }
            insertPendingFarmer(farmerId = "CUSTOM-FARMER", ageDays = 1)

            val atDay1 = custom.evaluate(USER_ID, NOW)
            assertEquals(1, atDay1.warningLevel)
            assertEquals(setOf(1), warningDays())

            /* The same record, three days old. */
            database.pendingSyncDao().getAllForUser(USER_ID)
            val atDay3 = custom.evaluate(USER_ID, NOW + 2 * DAY_MS)
            assertEquals(2, atDay3.warningLevel)
            assertEquals(setOf(1, 3), warningDays())

            val atDay5 = custom.evaluate(USER_ID, NOW + 4 * DAY_MS)
            assertEquals(3, atDay5.warningLevel)
            assertFalse(atDay5.accountLocked)
            assertEquals(setOf(1, 3, 5), warningDays())
            assertNotNull(database.farmerDao().getFarmerById("CUSTOM-FARMER"))
        }

    @Test
    fun laterWarningDays_doNotWarnEarly() =
        runBlocking {

            val custom = enforcerWith { SyncWarningPolicy.sanitize(listOf(3, 5, 6)) }
            insertPendingFarmer(farmerId = "LATE-FARMER", ageDays = 2)

            val result = custom.evaluate(USER_ID, NOW)

            assertEquals(0, result.warningLevel)
            assertEquals(emptySet<Int>(), warningDays())
        }

    @Test
    fun theWipeHappensAtDay7_evenWhenEveryWarningIsMovedEarly() =
        runBlocking {

            val early = enforcerWith { SyncWarningPolicy.sanitize(listOf(1, 2, 3)) }
            insertPendingFarmer(farmerId = "EARLY-FARMER", ageDays = 7)

            val result = early.evaluate(USER_ID, NOW)

            assertTrue(result.accountLocked)
            assertEquals(4, result.warningLevel)
            assertNull(database.farmerDao().getFarmerById("EARLY-FARMER"))
        }

    @Test
    fun theWipeHappensAtDay7_evenWhenEveryWarningIsMovedLate() =
        runBlocking {

            val late = enforcerWith { SyncWarningPolicy.sanitize(listOf(4, 5, 6)) }
            insertPendingFarmer(farmerId = "LATE-WIPE-FARMER", ageDays = 7)

            val result = late.evaluate(USER_ID, NOW)

            assertTrue(result.accountLocked)
            assertEquals(4, result.warningLevel)
            assertNull(database.farmerDao().getFarmerById("LATE-WIPE-FARMER"))
        }

    @Test
    fun nothingIsWipedBeforeDay7_whateverThePolicy() =
        runBlocking {

            insertPendingFarmer(farmerId = "SAFE-FARMER", ageDays = 6)

            listOf(
                SyncWarningPolicy.sanitize(listOf(1, 2, 3)),
                SyncWarningPolicy.sanitize(listOf(4, 5, 6)),
                SyncWarningPolicy.sanitize(listOf(6, 6, 6)),
                SyncWarningPolicy.sanitize(listOf(0, 4, 6)),
                SyncWarningPolicy.sanitize(listOf(2, 4, 7)),
                SyncWarningPolicy.sanitize(null),
                SyncWarningPolicy.DEFAULT
            ).forEach { policy ->

                val result = enforcerWith { policy }.evaluate(USER_ID, NOW)

                assertFalse("Locked under $policy", result.accountLocked)
                assertEquals(0, result.wipedOperationCount)
                assertNotNull(database.farmerDao().getFarmerById("SAFE-FARMER"))
                assertEquals(1, database.pendingSyncDao().getPendingCount())
            }
        }

    @Test
    fun aPolicyThatCannotBeLoaded_fallsBackToTheDefaultAndStillWipesAtDay7() =
        runBlocking {

            val broken = enforcerWith { error("device_config unreadable") }

            insertPendingFarmer(farmerId = "BROKEN-6", ageDays = 6)
            val atDay6 = broken.evaluate(USER_ID, NOW)
            assertEquals(3, atDay6.warningLevel)
            assertFalse(atDay6.accountLocked)
            assertEquals(setOf(2, 4, 6), warningDays())

            val atDay7 = broken.evaluate(USER_ID, NOW + DAY_MS)
            assertTrue(atDay7.accountLocked)
            assertNull(database.farmerDao().getFarmerById("BROKEN-6"))
        }

    @Test
    fun policyStore_roundTripsAndNeverReturnsAnInvalidPolicy() =
        runBlocking {

            val store = SyncPolicyStore(database.referenceDataDao())
            assertEquals(SyncWarningPolicy.DEFAULT, store.current())
            assertNull(store.version())

            store.save(listOf(1, 3, 5), version = 4, now = NOW)
            assertEquals(listOf(1, 3, 5), store.current().warningDays)
            assertEquals(4L, store.version())

            /* An invalid set from the server is stored as the default, and its version is remembered. */
            store.save(listOf(6, 6, 6), version = 5, now = NOW)
            assertEquals(SyncWarningPolicy.DEFAULT, store.current())
            assertEquals(5L, store.version())

            /* A hand-corrupted stored value reads as the default. */
            database.referenceDataDao().putConfig(
                com.beeftech.database.entity.DeviceConfigEntry(
                    com.beeftech.database.entity.DeviceConfigEntry.SYNC_WARNING_DAYS, "9,x,1", NOW
                )
            )
            assertEquals(SyncWarningPolicy.DEFAULT, store.current())
        }

    private suspend fun insertPendingFarmer(
        farmerId: String,
        ageDays: Long
    ) {

        database
            .farmerDao()
            .insertFarmer(
                testFarmer(
                    farmerId =
                        farmerId,

                    syncStatus =
                        "PENDING"
                )
            )

        database
            .pendingSyncDao()
            .insert(
                PendingSync(
                    userId = USER_ID,

                    entityType =
                        SyncSecurityDao
                            .ENTITY_FARMER_REGISTRATION,

                    entityId =
                        farmerId,

                    operation =
                        "CREATE",

                    payload =
                        farmerId,

                    createdAt =
                        NOW -
                                (
                                    ageDays *
                                            DAY_MS
                                    )
                )
            )
    }

    private fun testFarmer(
        farmerId: String,
        syncStatus: String
    ): FarmerEntity {

        return FarmerEntity(
            farmer_id =
                farmerId,

            client_code =
                farmerId,

            organisation_name =
                "Policy Test Farm",

            vat_number =
                null,

            email_address =
                null,

            gps_latitude =
                null,

            gps_longitude =
                null,

            sync_status =
                syncStatus
        )
    }

    private suspend fun warningDays():
            Set<Int> {

        return database
            .syncSecurityDao()
            .getAllEvents()
            .filter {
                it.eventType ==
                        SyncSecurityDao.EVENT_WARNING
            }
            .mapNotNull {
                it.warningDay
            }
            .toSet()
    }

    private fun createPassphrase():
            ByteArray {

        return ByteArray(32) {
                index ->

            (
                index +
                        1
                )
                .toByte()
        }
    }

    companion object {

        private const val DATABASE_NAME =
            "beeftech.db"

        private const val USER_ID =
            "SYNC-POLICY-TEST-USER"

        private const val SECOND_USER_ID =
            "SYNC-POLICY-SECOND-USER"

        private const val NOW =
            1_800_000_000_000L

        private const val DAY_MS =
            24L * 60L * 60L * 1000L
    }
}
