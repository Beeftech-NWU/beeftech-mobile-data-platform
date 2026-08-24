package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseProviderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        DatabaseProvider.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun tearDown() {
        DatabaseProvider.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun initialize_correctPassphrase_opensDatabase() {

        val passphrase =
            ByteArray(32) { index ->
                (index + 1).toByte()
            }

        val result =
            DatabaseProvider.initialize(
                context,
                passphrase
            )

        assertTrue(
            result is DatabaseResult.Success
        )

        assertTrue(
            DatabaseProvider.isOpen()
        )
    }

    @Test
    fun close_closesDatabase() {

        val passphrase =
            ByteArray(32) { index ->
                (index + 1).toByte()
            }

        DatabaseProvider.initialize(
            context,
            passphrase
        )

        DatabaseProvider.close()

        assertFalse(
            DatabaseProvider.isOpen()
        )
    }

    companion object {
        private const val DATABASE_NAME =
            "beeftech.db"
    }
}