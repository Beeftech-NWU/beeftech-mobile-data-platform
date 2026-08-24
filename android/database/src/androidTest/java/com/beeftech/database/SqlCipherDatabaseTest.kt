package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.AnimalMovement
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SqlCipherDatabaseTest {

    private lateinit var context: Context

    private var database: BeefTechDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Make sure every test starts with a clean database.
        database?.close()
        database = null
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun tearDown() {
        database?.close()
        database = null

        // Remove the test database after every test.
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun correctPassphrase_allowsDatabaseReadAndWrite() = runBlocking {

        val passphrase = createCorrectPassphrase()

        val result = DatabaseFactory.create(
            context = context,
            passphrase = passphrase
        )

        assertTrue(
            "Database should open with the correct passphrase.",
            result is DatabaseResult.Success
        )

        database =
            (result as DatabaseResult.Success).database

        val movement = AnimalMovement(
            animalId = "BT-001",
            movementType = "TRANSFER",
            timestamp = System.currentTimeMillis()
        )

        database!!
            .animalMovementDao()
            .insert(movement)

        val movements =
            database!!
                .animalMovementDao()
                .getAll()

        assertEquals(
            1,
            movements.size
        )

        assertEquals(
            "BT-001",
            movements[0].animalId
        )

        assertEquals(
            "TRANSFER",
            movements[0].movementType
        )
    }

    @Test
    fun wrongPassphrase_returnsInvalidPassphraseError() = runBlocking {

        val correctPassphrase = createCorrectPassphrase()

        val firstResult = DatabaseFactory.create(
            context = context,
            passphrase = correctPassphrase
        )

        assertTrue(
            "Initial database should open with the correct passphrase.",
            firstResult is DatabaseResult.Success
        )

        database =
            (firstResult as DatabaseResult.Success).database

        database!!
            .animalMovementDao()
            .insert(
                AnimalMovement(
                    animalId = "BT-002",
                    movementType = "SALE",
                    timestamp = System.currentTimeMillis()
                )
            )

        // Close the correctly encrypted database before reopening it.
        database!!.close()
        database = null

        val wrongPassphrase =
            ByteArray(32) {
                99.toByte()
            }

        val wrongResult = DatabaseFactory.create(
            context = context,
            passphrase = wrongPassphrase
        )

        assertTrue(
            "Opening an encrypted database with the wrong passphrase should fail.",
            wrongResult is DatabaseResult.Error
        )

        val error =
            wrongResult as DatabaseResult.Error

        assertEquals(
            DatabaseErrorType.INVALID_PASSPHRASE,
            error.type
        )
    }

    @Test
    fun emptyPassphrase_returnsEmptyPassphraseError() {

        val result = DatabaseFactory.create(
            context = context,
            passphrase = byteArrayOf()
        )

        assertTrue(
            "An empty passphrase should be rejected.",
            result is DatabaseResult.Error
        )

        val error =
            result as DatabaseResult.Error

        assertEquals(
            DatabaseErrorType.EMPTY_PASSPHRASE,
            error.type
        )
    }

    @Test
    fun corruptedDatabase_returnsDatabaseError() {

        val databaseFile =
            context.getDatabasePath(DATABASE_NAME)

        databaseFile.parentFile?.mkdirs()

        // Create deliberately invalid database content.
        databaseFile.writeBytes(
            byteArrayOf(
                1, 2, 3, 4,
                5, 6, 7, 8,
                9, 10, 11, 12,
                13, 14, 15, 16
            )
        )

        val passphrase = createCorrectPassphrase()

        val result = DatabaseFactory.create(
            context = context,
            passphrase = passphrase
        )

        assertTrue(
            "A corrupted database should return a controlled error.",
            result is DatabaseResult.Error
        )

        val error =
            result as DatabaseResult.Error

        /*
         * SQLCipher can report corrupted encrypted data similarly to
         * an incorrect encryption key. The exact classification can
         * be tightened once the tests are run against the project's
         * final SQLCipher version.
         */
        assertTrue(
            "Unexpected database error type: ${error.type}",
            error.type == DatabaseErrorType.DATABASE_CORRUPTION ||
                    error.type == DatabaseErrorType.INVALID_PASSPHRASE ||
                    error.type == DatabaseErrorType.DATABASE_OPEN_ERROR
        )
    }

    private fun createCorrectPassphrase(): ByteArray {
        return ByteArray(32) { index ->
            (index + 1).toByte()
        }
    }

    companion object {
        private const val DATABASE_NAME = "beeftech.db"
    }
}