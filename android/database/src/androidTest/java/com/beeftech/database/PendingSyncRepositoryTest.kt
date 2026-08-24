package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.repository.PendingSyncRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PendingSyncRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: BeefTechDatabase
    private lateinit var repository: PendingSyncRepository

    @Before
    fun setUp() {

        context = ApplicationProvider.getApplicationContext()

        context.deleteDatabase(DATABASE_NAME)

        val passphrase =
            ByteArray(32) { index ->
                (index + 1).toByte()
            }

        val result =
            DatabaseFactory.create(
                context,
                passphrase
            )

        assertTrue(
            "Encrypted database should open successfully.",
            result is DatabaseResult.Success
        )

        database =
            (result as DatabaseResult.Success).database

        repository =
            PendingSyncRepository(
                database.pendingSyncDao()
            )
    }

    @After
    fun tearDown() {

        if (::database.isInitialized) {
            database.close()
        }

        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun queuedOperation_isStoredLocally() = runBlocking {

        repository.queueOperation(
            entityType = "ANIMAL_MOVEMENT",
            entityId = "BT-001",
            operation = "INSERT",
            payload = """{"animalId":"BT-001"}"""
        )

        val pending =
            repository.getPendingOperations()

        assertEquals(
            1,
            pending.size
        )

        assertEquals(
            "ANIMAL_MOVEMENT",
            pending[0].entityType
        )

        assertEquals(
            "BT-001",
            pending[0].entityId
        )

        assertEquals(
            0,
            pending[0].retryCount
        )
    }

    @Test
    fun failedSync_increasesRetryCount() = runBlocking {

        val id =
            repository.queueOperation(
                entityType = "TREATMENT",
                entityId = "BT-002",
                operation = "INSERT",
                payload = """{"animalId":"BT-002"}"""
            )

        repository.markSyncFailed(id)

        val pending =
            repository.getPendingOperations()

        assertEquals(
            1,
            pending.size
        )

        assertEquals(
            1,
            pending[0].retryCount
        )
    }

    @Test
    fun successfulSync_removesOperation() = runBlocking {

        val id =
            repository.queueOperation(
                entityType = "MORTALITY",
                entityId = "BT-003",
                operation = "INSERT",
                payload = """{"animalId":"BT-003"}"""
            )

        assertEquals(
            1,
            repository.getPendingCount()
        )

        repository.markSyncSuccessful(id)

        assertEquals(
            0,
            repository.getPendingCount()
        )
    }

    @Test
    fun operationStopsBeingReturned_afterMaximumRetries() = runBlocking {

        val id =
            repository.queueOperation(
                entityType = "ANIMAL_MOVEMENT",
                entityId = "BT-004",
                operation = "INSERT",
                payload = """{"animalId":"BT-004"}"""
            )

        repeat(
            PendingSyncRepository.DEFAULT_MAX_RETRIES
        ) {
            repository.markSyncFailed(id)
        }

        val pending =
            repository.getPendingOperations()

        assertTrue(
            pending.none { it.id == id }
        )
    }

    companion object {
        private const val DATABASE_NAME =
            "beeftech.db"
    }
}