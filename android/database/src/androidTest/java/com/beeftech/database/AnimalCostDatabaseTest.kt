package com.beeftech.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.AnimalCost
import com.beeftech.database.entity.CostType
import com.beeftech.database.entity.Treatment
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

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

        context.deleteDatabase(
            databaseName
        )

        System.loadLibrary(
            "sqlcipher"
        )

        database =
            openDatabase()
    }

    /*
     * Opens (or reopens) the encrypted test database
     * with the same seed callback production uses.
     */
    private fun openDatabase(): BeefTechDatabase {

        val context =
            ApplicationProvider
                .getApplicationContext<Context>()

        val factory =
            SupportOpenHelperFactory(
                passphrase
            )

        val opened =
            Room.databaseBuilder(
                context,
                BeefTechDatabase::class.java,
                databaseName
            )
                .openHelperFactory(
                    factory
                )
                .addCallback(
                    BeefTechDatabase.SEED_CALLBACK
                )
                .build()

        opened
            .openHelper
            .writableDatabase

        return opened
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

    @Test
    fun calculateCompleteAnimalCostSummary() =
        runBlocking {

            val animalCostDao =
                database.animalCostDao()

            val treatmentDao =
                database.treatmentDao()

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

            treatmentDao.insertWithCost(
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

                    gpsLat =
                        -26.2041,

                    gpsLng =
                        28.0473,

                    timestamp =
                        5000L
                )
            )

            val totalsMap = animalCostDao.getTotalsByType("TEST-001").associate { it.costType to it.total }

            val transport = totalsMap["TRANSPORT"] ?: 0.0
            val processing = totalsMap["PROCESSING"] ?: 0.0
            val treatment = totalsMap["TREATMENT"] ?: 0.0
            val handling = totalsMap["HANDLING"] ?: 0.0
            val interest = totalsMap["INTEREST"] ?: 0.0
            val total = totalsMap.values.sum()

            assertEquals(250.0, transport, 0.001)
            assertEquals(100.0, processing, 0.001)
            assertEquals(150.0, treatment, 0.001)
            assertEquals(50.0, handling, 0.001)
            assertEquals(25.0, interest, 0.001)
            assertEquals(575.0, total, 0.001)
        }

    @Test(expected = Exception::class)
    fun insertDuplicateRecordGuid_throwsException() =
        runBlocking {
            val dao = database.animalCostDao()
            val guid = UUID.randomUUID().toString()
            dao.insert(
                AnimalCost(
                    animalId = "TEST-001",
                    costType = "TRANSPORT",
                    amount = 100.0,
                    gpsLat = -26.0,
                    gpsLng = 28.0,
                    timestamp = 1000L,
                    recordGuid = guid
                )
            )
            dao.insert(
                AnimalCost(
                    animalId = "TEST-001",
                    costType = "PROCESSING",
                    amount = 50.0,
                    gpsLat = -26.0,
                    gpsLng = 28.0,
                    timestamp = 2000L,
                    recordGuid = guid
                )
            )
        }

    @Test(expected = Exception::class)
    fun insertInvalidCostType_throwsForeignKeyException() =
        runBlocking {
            val dao = database.animalCostDao()
            dao.insert(
                AnimalCost(
                    animalId = "TEST-001",
                    costType = "NOPE",
                    amount = 100.0,
                    gpsLat = -26.0,
                    gpsLng = 28.0,
                    timestamp = 1000L
                )
            )
        }

    @Test
    fun addNewCostType_allowsInsertingCostOfNewType() =
        runBlocking {
            val costTypeDao = database.costTypeDao()
            val animalCostDao = database.animalCostDao()

            costTypeDao.insertAll(
                listOf(
                    CostType(
                        code = "CUSTOM_LABOR",
                        displayName = "Custom Labor",
                        sortOrder = 10
                    )
                )
            )

            animalCostDao.insert(
                AnimalCost(
                    animalId = "TEST-001",
                    costType = "CUSTOM_LABOR",
                    amount = 300.0,
                    gpsLat = -26.0,
                    gpsLng = 28.0,
                    timestamp = 1000L
                )
            )

            val totals = animalCostDao.getTotalsByType("TEST-001")
            val customLaborTotal = totals.find { it.costType == "CUSTOM_LABOR" }?.total
            assertEquals(300.0, customLaborTotal!!, 0.001)
        }

    /*
     * A destructive migration recreates the tables without
     * calling onCreate, leaving cost_types empty. The seed
     * callback must restore it the next time the database opens.
     */
    @Test
    fun emptiedCostTypes_areReseededOnNextOpen() =
        runBlocking {
            database
                .openHelper
                .writableDatabase
                .execSQL("DELETE FROM cost_types")

            assertTrue(
                database.costTypeDao().getActive().isEmpty()
            )

            database.close()

            database =
                openDatabase()

            val codes =
                database
                    .costTypeDao()
                    .getActive()
                    .map { it.code }

            CostTypeSeed.TYPES.forEach { (code, _) ->
                assertTrue(
                    "cost_types must contain seed code $code after reopening",
                    codes.contains(code)
                )
            }
        }
}
