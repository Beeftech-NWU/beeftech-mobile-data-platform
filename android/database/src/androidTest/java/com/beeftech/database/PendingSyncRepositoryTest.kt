package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.database.security.CurrentUserIdRegistry
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

        CurrentUserIdRegistry
            .setCurrentUserId(
                TEST_USER_ID
            )

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

        CurrentUserIdRegistry.clear()


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


    @Test
    fun activeUserQueue_isIsolatedWhenUsersSwitch() =
        runBlocking {

            CurrentUserIdRegistry
                .setCurrentUserId(
                    TEST_USER_ID
                )

            val userAOperationId =
                repository.queueOperation(
                    entityType =
                        "TREATMENT",

                    entityId =
                        "USER-A-TREATMENT",

                    operation =
                        "CREATE",

                    payload =
                        "USER-A-TREATMENT"
                )


            CurrentUserIdRegistry
                .setCurrentUserId(
                    SECOND_USER_ID
                )

            val userBOperationId =
                repository.queueOperation(
                    entityType =
                        "TREATMENT",

                    entityId =
                        "USER-B-TREATMENT",

                    operation =
                        "CREATE",

                    payload =
                        "USER-B-TREATMENT"
                )


            val userBOperations =
                repository
                    .getAllPendingOperations()

            assertEquals(
                1,
                userBOperations.size
            )

            assertEquals(
                userBOperationId,
                userBOperations
                    .single()
                    .id
            )

            assertEquals(
                "USER-B-TREATMENT",
                userBOperations
                    .single()
                    .entityId
            )


            CurrentUserIdRegistry
                .setCurrentUserId(
                    TEST_USER_ID
                )

            val userAOperations =
                repository
                    .getAllPendingOperations()

            assertEquals(
                1,
                userAOperations.size
            )

            assertEquals(
                userAOperationId,
                userAOperations
                    .single()
                    .id
            )


            /*
             * USER-A cleanup must leave USER-B untouched.
             */
            repository
                .markSyncSuccessful(
                    userAOperationId
                )

            assertEquals(
                0,
                repository.getPendingCount()
            )


            CurrentUserIdRegistry
                .setCurrentUserId(
                    SECOND_USER_ID
                )

            assertEquals(
                1,
                repository.getPendingCount()
            )

            assertEquals(
                userBOperationId,
                repository
                    .getAllPendingOperations()
                    .single()
                    .id
            )
        }


    companion object {
        private const val DATABASE_NAME =
            "beeftech.db"

        private const val TEST_USER_ID =
            "PENDING-REPOSITORY-USER-A"

        private const val SECOND_USER_ID =
            "PENDING-REPOSITORY-USER-B"
    }
}