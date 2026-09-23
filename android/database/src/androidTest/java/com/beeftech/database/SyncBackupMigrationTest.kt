package com.beeftech.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncBackupMigrationTest {

    private lateinit var context: Context
    private val databaseName = "sync_backup_test.db"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun testSyncBackupMigrationAddsSyncStatusColumn() {
        val helperFactory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(object : SupportSQLiteOpenHelper.Callback(11) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `sync_backups` (`id` TEXT NOT NULL, `batchId` TEXT NOT NULL, PRIMARY KEY(`id`))")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val openHelper = helperFactory.create(config)
        val db = openHelper.writableDatabase
        db.execSQL("INSERT INTO `sync_backups` (`id`, `batchId`) VALUES ('backup-1', 'batch-1')")
        db.close()

        val configForMigration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(object : SupportSQLiteOpenHelper.Callback(12) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 11 && newVersion == 12) {
                        BeefTechDatabase.MIGRATION_11_12.migrate(db)
                    }
                }
            })
            .build()

        val migratedDb = helperFactory.create(configForMigration).writableDatabase
        val cursor = migratedDb.query("PRAGMA table_info(`sync_backups`)")
        var hasSyncStatusColumn = false
        while (cursor.moveToNext()) {
            val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            if (columnName == "sync_status") {
                hasSyncStatusColumn = true
            }
        }
        cursor.close()
        migratedDb.close()

        assertTrue("sync_backups table must contain sync_status column after migration", hasSyncStatusColumn)
    }
}
