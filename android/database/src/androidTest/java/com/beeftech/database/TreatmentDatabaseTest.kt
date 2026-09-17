package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.Treatment
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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

    companion object {

        private const val DATABASE_NAME =
            "beeftech.db"
    }
}