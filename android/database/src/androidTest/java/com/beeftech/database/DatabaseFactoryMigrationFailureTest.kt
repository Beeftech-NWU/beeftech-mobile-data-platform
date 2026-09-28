package com.beeftech.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * R0.1/R0.7: opening a database whose on-disk user_version Room doesn't
 * recognise (no migration path, no destructive fallback) must fail loudly
 * with DatabaseErrorType.MIGRATION_FAILED and leave the file exactly as it
 * was, never fall back to wiping it.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseFactoryMigrationFailureTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        System.loadLibrary("sqlcipher")
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun create_unknownFutureVersion_returnsMigrationFailed_andLeavesFileUntouched() {
        val passphrase = ByteArray(32) { index -> (index + 1).toByte() }

        // Write a database at a version Room has no migration path for (99,
        // far ahead of the latest declared version), with a marker row so a
        // silent wipe would be detectable.
        val cipherFactory = SupportOpenHelperFactory(passphrase)
        val futureVersionConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(DATABASE_NAME)
            .callback(object : SupportSQLiteOpenHelper.Callback(FUTURE_VERSION) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE `marker` (`id` INTEGER PRIMARY KEY NOT NULL)")
                    db.execSQL("INSERT INTO `marker` (`id`) VALUES (1)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        cipherFactory.create(futureVersionConfig).writableDatabase.close()

        // DatabaseFactory must refuse to open it, and must not wipe it.
        val result = DatabaseFactory.create(context = context, passphrase = passphrase)

        assertTrue("Expected a DatabaseResult.Error, got $result", result is DatabaseResult.Error)
        assertEquals(DatabaseErrorType.MIGRATION_FAILED, (result as DatabaseResult.Error).type)

        // The file must be exactly as it was: still at version 99, marker row intact.
        val reopened = cipherFactory.create(futureVersionConfig).writableDatabase
        assertEquals(FUTURE_VERSION, reopened.version)
        reopened.query("SELECT COUNT(*) FROM `marker`").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
        }
        reopened.close()
    }

    companion object {
        private const val DATABASE_NAME = "beeftech.db"
        private const val FUTURE_VERSION = 99
    }
}
