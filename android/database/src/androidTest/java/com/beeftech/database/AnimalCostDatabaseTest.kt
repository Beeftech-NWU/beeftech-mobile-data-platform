package com.beeftech.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.AnimalCost
import com.beeftech.database.entity.LocationFeed
import com.beeftech.database.entity.Treatment
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnimalCostDatabaseTest {

    private lateinit var database: BeefTechDatabase

    private val databaseName =
        "animal-cost-test.db"

    private val passphrase =
        "animal-cost-test-passphrase"
            .toByteArray(Charsets.UTF_8)

    @Before
    fun setup() {

        val context =
            ApplicationProvider
                .getApplicationContext<Context>()

        /*
         * Remove any database left over
         * from a previous test run.
         */
        context.deleteDatabase(
            databaseName
        )

        /*
         * Load SQLCipher.
         */
        System.loadLibrary(
            "sqlcipher"
        )

        val factory =
            SupportOpenHelperFactory(
                passphrase
            )

        /*
         * Create a real encrypted test database.
         */
        database =
            Room.databaseBuilder(
                context,
                BeefTechDatabase::class.java,
                databaseName
            )
                .openHelperFactory(
                    factory
                )
                .build()

        /*
         * Force the encrypted database to open.
         */
        database
            .openHelper
            .writableDatabase
    }

    @After
    fun tearDown() {

        database.close()

        val context =
            ApplicationProvider
                .getApplicationContext<Context>()

        context.deleteDatabase(
            databaseName
        )
    }

    /*
     * TEST 1
     *
     * Confirm that AnimalCost records can be
     * inserted and retrieved from SQLCipher.
     */
    @Test
    fun insertAnimalCosts_andReadThemBack() =
        runBlocking {

            val dao =
                database.animalCostDao()

            dao.insert(
                AnimalCost(
                    animalId =
                        "TEST-001",

                    costType =
                        "TRANSPORT",

                    amount =
                        250.0,

                    description =
                        "Transport to feedlot",

                    gpsLat =
                        -26.2041,

                    gpsLng =
                        28.0473,

                    timestamp =
                        1000L
                )
            )

            dao.insert(
                AnimalCost(
                    animalId =
                        "TEST-001",

                    costType =
                        "PROCESSING",

                    amount =
                        100.0,

                    description =
                        "Processing cost",

                    gpsLat =
                        -26.2041,

                    gpsLng =
                        28.0473,

                    timestamp =
                        2000L
                )
            )

            val records =
                dao.getByAnimalId(
                    "TEST-001"
                )

            assertEquals(
                2,
                records.size
            )

            assertTrue(
                records.any {
                    it.costType ==
                            "TRANSPORT" &&
                            it.amount ==
                            250.0
                }
            )

            assertTrue(
                records.any {
                    it.costType ==
                            "PROCESSING" &&
                            it.amount ==
                            100.0
                }
            )
        }

    /*
     * TEST 2
     *
     * Confirm that costs are summed by type.
     */
    @Test
    fun calculateTotalCostByType() =
        runBlocking {

            val dao =
                database.animalCostDao()

            dao.insert(
                AnimalCost(
                    animalId =
                        "TEST-001",

                    costType =
                        "TRANSPORT",

                    amount =
                        200.0,

                    description =
                        "Trip 1",

                    gpsLat =
                        -26.2041,

                    gpsLng =
                        28.0473,

                    timestamp =
                        1000L
                )
            )

            dao.insert(
                AnimalCost(
                    animalId =
                        "TEST-001",

                    costType =
                        "TRANSPORT",

                    amount =
                        50.0,

                    description =
                        "Trip 2",

                    gpsLat =
                        -26.2041,

                    gpsLng =
                        28.0473,

                    timestamp =
                        2000L
                )
            )

            val transportTotal =
                dao.getTotalByType(
                    animalId =
                        "TEST-001",

                    costType =
                        "TRANSPORT"
                )

            assertEquals(
                250.0,
                transportTotal,
                0.001
            )
        }

    /*
     * TEST 3
     *
     * Confirm that one animal's costs
     * cannot be mixed with another animal.
     */
    @Test
    fun costsDoNotMixBetweenAnimals() =
        runBlocking {

            val dao =
                database.animalCostDao()

            dao.insert(
                AnimalCost(
                    animalId =
                        "TEST-001",

                    costType =
                        "HANDLING",

                    amount =
                        50.0,

                    description =
                        "TEST-001 handling",

                    gpsLat =
                        -26.2041,

                    gpsLng =
                        28.0473,

                    timestamp =
                        1000L
                )
            )

            dao.insert(
                AnimalCost(
                    animalId =
                        "TEST-002",

                    costType =
                        "HANDLING",

                    amount =
                        900.0,

                    description =
                        "TEST-002 handling",

                    gpsLat =
                        -26.2041,

                    gpsLng =
                        28.0473,

                    timestamp =
                        2000L
                )
            )

            val test001Total =
                dao.getTotalByType(
                    animalId =
                        "TEST-001",

                    costType =
                        "HANDLING"
                )

            val test002Total =
                dao.getTotalByType(
                    animalId =
                        "TEST-002",

                    costType =
                        "HANDLING"
                )

            assertEquals(
                50.0,
                test001Total,
                0.001
            )

            assertEquals(
                900.0,
                test002Total,
                0.001
            )
        }

    /*
     * TEST 4
     *
     * Test the same calculation that the
     * Cost Summary backend performs.
     *
     * Transport   = R250
     * Processing  = R100
     * Treatment   = R150
     * Feed        = R500
     * Handling    = R50
     * Interest    = R25
     *
     * Expected total = R1 075
     */
    @Test
    fun calculateCompleteAnimalCostSummary() =
        runBlocking {

            val animalCostDao =
                database.animalCostDao()

            val treatmentDao =
                database.treatmentDao()

            /*
             * Transport
             */
            animalCostDao.insert(
                AnimalCost(
                    animalId =
                        "TEST-001",

                    costType =
                        "TRANSPORT",

                    amount =
                        250.0,

                    description =
                        "Transport",

                    gpsLat =
                        -26.2041,

                    gpsLng =
                        28.0473,

                    timestamp =
                        1000L
                )
            )

            /*
             * Processing
             */
            animalCostDao.insert(
                AnimalCost(
                    animalId =
                        "TEST-001",

                    costType =
                        "PROCESSING",

                    amount =
                        100.0,

                    description =
                        "Processing",

                    gpsLat =
                        -26.2041,

                    gpsLng =
                        28.0473,

                    timestamp =
                        2000L
                )
            )

            /*
             * Handling
             */
            animalCostDao.insert(
                AnimalCost(
                    animalId =
                        "TEST-001",

                    costType =
                        "HANDLING",

                    amount =
                        50.0,

                    description =
                        "Handling",

                    gpsLat =
                        -26.2041,

                    gpsLng =
                        28.0473,

                    timestamp =
                        3000L
                )
            )

            /*
             * Interest
             */
            animalCostDao.insert(
                AnimalCost(
                    animalId =
                        "TEST-001",

                    costType =
                        "INTEREST",

                    amount =
                        25.0,

                    description =
                        "Interest",

                    gpsLat =
                        -26.2041,

                    gpsLng =
                        28.0473,

                    timestamp =
                        4000L
                )
            )

            /*
             * Treatment = R150
             */
            treatmentDao.insert(
                Treatment(
                    animalId =
                        "TEST-001",

                    disease =
                        "Respiratory infection",

                    treatmentName =
                        "Antibiotic",

                    batchNumber =
                        "BATCH-001",

                    volumeUsed =
                        "10 ml",

                    cost =
                        150.0,

                    timestamp =
                        5000L
                )
            )

            val transport =
                animalCostDao.getTotalByType(
                    "TEST-001",
                    "TRANSPORT"
                )

            val processing =
                animalCostDao.getTotalByType(
                    "TEST-001",
                    "PROCESSING"
                )

            val treatment =
                treatmentDao
                    .getTotalCostByAnimalId(
                        "TEST-001"
                    )

            val handling =
                animalCostDao.getTotalByType(
                    "TEST-001",
                    "HANDLING"
                )

            val interest =
                animalCostDao.getTotalByType(
                    "TEST-001",
                    "INTEREST"
                )

            val total =
                transport +
                        processing +
                        treatment +
                        handling +
                        interest

            assertEquals(
                250.0,
                transport,
                0.001
            )

            assertEquals(
                100.0,
                processing,
                0.001
            )

            assertEquals(
                150.0,
                treatment,
                0.001
            )

            assertEquals(
                50.0,
                handling,
                0.001
            )

            assertEquals(
                25.0,
                interest,
                0.001
            )

            assertEquals(
                575.0,
                total,
                0.001
            )
        }
}
