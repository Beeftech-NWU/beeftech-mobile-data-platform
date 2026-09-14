package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.AnimalMovement
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnimalMovementDatabaseTest {

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
    fun insertMovement_savesMovementRecord() =
        runBlocking {

            val database =
                DatabaseProvider.getDatabase()

            assertNotNull(database)

            val dao =
                database!!.animalMovementDao()

            val movement =
                AnimalMovement(
                    animalId = "TEST-001",
                    movementType = "Moved from Camp A to Camp B",
                    responsibleWorker = "Worker 01",
                    timestamp = 1725148800000L
                )

            dao.insert(movement)

            val movements =
                dao.getAll()

            assertEquals(
                1,
                movements.size
            )

            val savedMovement =
                movements.first()

            assertTrue(
                savedMovement.id > 0
            )

            assertEquals(
                "TEST-001",
                savedMovement.animalId
            )

            assertEquals(
                "Moved from Camp A to Camp B",
                savedMovement.movementType
            )

            assertEquals(
                "Worker 01",
                savedMovement.responsibleWorker
            )

            assertEquals(
                1725148800000L,
                savedMovement.timestamp
            )
        }

    @Test
    fun insertMultipleMovements_returnsAllMovements() =
        runBlocking {

            val database =
                DatabaseProvider.getDatabase()

            assertNotNull(database)

            val dao =
                database!!.animalMovementDao()

            val movementOne =
                AnimalMovement(
                    animalId = "TEST-001",
                    movementType = "Moved to Camp B",
                    responsibleWorker = "Worker 01",
                    timestamp = 1725148800000L
                )

            val movementTwo =
                AnimalMovement(
                    animalId = "TEST-002",
                    movementType = "Moved to Holding Area",
                    responsibleWorker = "Worker 02",
                    timestamp = 1725235200000L
                )

            dao.insert(movementOne)
            dao.insert(movementTwo)

            val movements =
                dao.getAll()

            assertEquals(
                2,
                movements.size
            )
        }

    companion object {

        private const val DATABASE_NAME =
            "beeftech.db"
    }
}