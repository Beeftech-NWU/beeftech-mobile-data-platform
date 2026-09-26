package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.AnimalCost
import com.beeftech.database.entity.CostSource
import com.beeftech.database.entity.Treatment
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TreatmentDatabaseTest {

    private lateinit var context: Context

    @Before
    fun setUp() {

        context =
            ApplicationProvider.getApplicationContext()

        DatabaseProvider.close()

        context.deleteDatabase(DATABASE_NAME)

        val passphrase =
            ByteArray(32) { index ->
                (index + 1).toByte()
            }

        val result =
            DatabaseProvider.initialize(
                context = context,
                passphrase = passphrase
            )

        check(result is DatabaseResult.Success) {
            "Database could not be initialized."
        }
    }

    @After
    fun tearDown() {

        DatabaseProvider.close()

        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun insertTreatment_savesTreatmentRecord() =
        runBlocking {

            val database =
                DatabaseProvider.getDatabase()

            assertNotNull(database)

            val dao =
                database!!.treatmentDao()

            val treatment =
                Treatment(
                    animalId = "TEST-001",
                    disease = "Bovine respiratory disease",
                    treatmentName = "Antibiotic treatment",
                    batchNumber = "BATCH-001",
                    volumeUsed = "10 ml",
                    cost = 150.00,
                    gpsLat = -26.0,
                    gpsLng = 28.0,
                    timestamp = 1725148800000L
                )

            dao.insert(treatment)

            val treatments =
                dao.getByAnimalId(
                    "TEST-001"
                )

            assertEquals(
                1,
                treatments.size
            )

            val savedTreatment =
                treatments.first()

            assertTrue(
                savedTreatment.id > 0
            )

            assertEquals(
                "TEST-001",
                savedTreatment.animalId
            )

            assertEquals(
                "Bovine respiratory disease",
                savedTreatment.disease
            )

            assertEquals(
                "Antibiotic treatment",
                savedTreatment.treatmentName
            )

            assertEquals(
                "BATCH-001",
                savedTreatment.batchNumber
            )

            assertEquals(
                "10 ml",
                savedTreatment.volumeUsed
            )

            assertEquals(
                150.00,
                savedTreatment.cost,
                0.001
            )
        }

    @Test
    fun getByAnimalId_returnsOnlySelectedAnimalTreatments() =
        runBlocking {

            val database =
                DatabaseProvider.getDatabase()

            assertNotNull(database)

            val dao =
                database!!.treatmentDao()

            dao.insert(
                Treatment(
                    animalId = "TEST-001",
                    disease = "Disease A",
                    treatmentName = "Treatment A",
                    batchNumber = "B001",
                    volumeUsed = "5 ml",
                    cost = 100.0,
                    gpsLat = -26.0,
                    gpsLng = 28.0,
                    timestamp = 1725148800000L
                )
            )

            dao.insert(
                Treatment(
                    animalId = "TEST-002",
                    disease = "Disease B",
                    treatmentName = "Treatment B",
                    batchNumber = "B002",
                    volumeUsed = "8 ml",
                    cost = 200.0,
                    gpsLat = -26.0,
                    gpsLng = 28.0,
                    timestamp = 1725235200000L
                )
            )

            val treatments =
                dao.getByAnimalId(
                    "TEST-001"
                )

            assertEquals(
                1,
                treatments.size
            )

            assertEquals(
                "TEST-001",
                treatments.first().animalId
            )
        }

    @Test
    fun insertWithCost_createsAnimalCostRowForPositiveCost() =
        runBlocking {
            val database = DatabaseProvider.getDatabase()!!
            val treatmentDao = database.treatmentDao()
            val animalCostDao = database.animalCostDao()

            val treatmentGuid = UUID.randomUUID().toString()
            val treatment = Treatment(
                animalId = "TEST-001",
                disease = "Disease C",
                treatmentName = "Treatment C",
                batchNumber = "B003",
                volumeUsed = "10 ml",
                cost = 120.0,
                gpsLat = -26.0,
                gpsLng = 28.0,
                timestamp = 1725300000000L,
                recordguid = treatmentGuid
            )

            treatmentDao.insertWithCost(treatment)

            val costs = animalCostDao.getByAnimalId("TEST-001")
            assertEquals(1, costs.size)

            val costRow = costs.first()
            assertEquals("TREATMENT", costRow.costType)
            assertEquals(120.0, costRow.amount, 0.001)
            assertEquals("Treatment C", costRow.description)
            assertEquals(CostSource.TREATMENT, costRow.sourceEntity)
            assertEquals(treatmentGuid, costRow.sourceRecordId)
        }

    @Test
    fun insertWithCost_createsNoAnimalCostRowForZeroCost() =
        runBlocking {
            val database = DatabaseProvider.getDatabase()!!
            val treatmentDao = database.treatmentDao()
            val animalCostDao = database.animalCostDao()

            val treatment = Treatment(
                animalId = "TEST-002",
                disease = "Disease D",
                treatmentName = "Treatment D",
                batchNumber = "B004",
                volumeUsed = "0 ml",
                cost = 0.0,
                gpsLat = -26.0,
                gpsLng = 28.0,
                timestamp = 1725300000000L
            )

            treatmentDao.insertWithCost(treatment)

            val costs = animalCostDao.getByAnimalId("TEST-002")
            assertEquals(0, costs.size)
        }

    @Test
    fun insertDerivedCost_isIdempotent() =
        runBlocking {
            val database = DatabaseProvider.getDatabase()!!
            val treatmentDao = database.treatmentDao()
            val animalCostDao = database.animalCostDao()

            val sourceGuid = UUID.randomUUID().toString()
            val costRow = AnimalCost(
                animalId = "TEST-003",
                costType = "TREATMENT",
                amount = 80.0,
                description = "Dewormer",
                gpsLat = -26.0,
                gpsLng = 28.0,
                timestamp = 1725300000000L,
                sourceEntity = CostSource.TREATMENT,
                sourceRecordId = sourceGuid
            )

            treatmentDao.insertDerivedCost(costRow)
            treatmentDao.insertDerivedCost(costRow)

            val costs = animalCostDao.getByAnimalId("TEST-003")
            assertEquals(1, costs.size)
        }

    companion object {

        private const val DATABASE_NAME =
            "beeftech.db"
    }
}
