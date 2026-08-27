package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeyStoreSecurityProviderTest {

    private lateinit var provider:
            AndroidKeyStoreSecurityProvider

    @Before
    fun setUp() {

        val context =
            ApplicationProvider
                .getApplicationContext<Context>()

        provider =
            AndroidKeyStoreSecurityProvider(context)

        provider.invalidateDatabaseKey()
    }

    @After
    fun tearDown() {

        try {
            provider.invalidateDatabaseKey()
        } catch (_: Exception) {
        }
    }

    @Test
    fun initializeKey_createsDatabaseSecurity() {

        provider.initializeKey(
            "DEV_ONLY_123456"
        )

        assertTrue(
            provider.isKeyAvailable()
        )

        val passphrase =
            provider.getDatabasePassphrase(
                "DEV_ONLY_123456"
            )

        try {

            assertNotNull(passphrase)

            assertTrue(
                passphrase.isNotEmpty()
            )

            assertTrue(
                passphrase.size == 32
            )

        } finally {

            passphrase.fill(0)
        }
    }

    @Test
    fun samePassphraseCanBeRecovered() {

        provider.initializeKey(
            "DEV_ONLY_123456"
        )

        val first =
            provider.getDatabasePassphrase(
                "DEV_ONLY_123456"
            )

        val firstCopy =
            first.copyOf()

        first.fill(0)

        val second =
            provider.getDatabasePassphrase(
                "DEV_ONLY_123456"
            )

        try {

            assertArrayEquals(
                firstCopy,
                second
            )

        } finally {

            firstCopy.fill(0)
            second.fill(0)
        }
    }

    @Test
    fun incorrectPasscodeIsRejected() {

        provider.initializeKey(
            "DEV_ONLY_123456"
        )

        try {

            provider.getDatabasePassphrase(
                "WRONG_PASSWORD"
            )

            fail(
                "Incorrect passcode must be rejected."
            )

        } catch (
            exception: DatabaseSecurityException
        ) {

            assertTrue(
                exception.message
                    ?.contains("authentication") == true
            )
        }
    }

    @Test
    fun clearKeyFromMemoryDoesNotDeleteKeyStoreKey() {

        provider.initializeKey(
            "DEV_ONLY_123456"
        )

        val passphrase =
            provider.getDatabasePassphrase(
                "DEV_ONLY_123456"
            )

        passphrase.fill(0)

        provider.clearKeyFromMemory()

        assertTrue(
            provider.isKeyAvailable()
        )
    }

    @Test
    fun invalidateDatabaseKeyDeletesKey() {

        provider.initializeKey(
            "DEV_ONLY_123456"
        )

        assertTrue(
            provider.isKeyAvailable()
        )

        provider.invalidateDatabaseKey()

        assertFalse(
            provider.isKeyAvailable()
        )

        try {

            provider.getDatabasePassphrase(
                "DEV_ONLY_123456"
            )

            fail(
                "Passphrase should not be available after invalidation."
            )

        } catch (
            exception: DatabaseSecurityException
        ) {

        }
    }
}
