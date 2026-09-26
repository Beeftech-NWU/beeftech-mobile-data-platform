package com.beeftech.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object DatabaseFactory {

    /*
     * Version 1 -> 2
     *
     * Adds the responsible worker field
     * to Animal Movement records.
     */
    private val MIGRATION_1_2 =
        object : Migration(1, 2) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {

                db.execSQL(
                    """
                    ALTER TABLE animal_movements
                    ADD COLUMN responsibleWorker TEXT NOT NULL DEFAULT ''
                    """.trimIndent()
                )
            }
        }

    /*
     * Version 2 -> 3
     *
     * Updates the Treatment table so that it matches
     * the Farm Traceability Treatments screen.
     */
    private val MIGRATION_2_3 =
        object : Migration(2, 3) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {

                db.execSQL(
                    """
                    CREATE TABLE treatments_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        animalId TEXT NOT NULL,
                        disease TEXT NOT NULL,
                        treatmentName TEXT NOT NULL,
                        batchNumber TEXT NOT NULL,
                        volumeUsed TEXT NOT NULL,
                        cost REAL NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO treatments_new (
                        id,
                        animalId,
                        disease,
                        treatmentName,
                        batchNumber,
                        volumeUsed,
                        cost,
                        timestamp
                    )
                    SELECT
                        id,
                        animalId,
                        treatmentType,
                        COALESCE(medication, ''),
                        '',
                        '',
                        0.0,
                        timestamp
                    FROM treatments
                    """.trimIndent()
                )

                db.execSQL(
                    "DROP TABLE treatments"
                )

                db.execSQL(
                    """
                    ALTER TABLE treatments_new
                    RENAME TO treatments
                    """.trimIndent()
                )
            }
        }

    /*
     * Version 3 -> 4
     *
     * Adds the responsible worker field
     * to Mortality records.
     */
    private val MIGRATION_3_4 =
        object : Migration(3, 4) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {

                db.execSQL(
                    """
                    ALTER TABLE mortalities
                    ADD COLUMN responsibleWorker TEXT NOT NULL DEFAULT ''
                    """.trimIndent()
                )
            }
        }

    /*
     * Version 4 -> 5
     *
     * Adds the Supplier table.
     */
    private val MIGRATION_4_5 =
        object : Migration(4, 5) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS suppliers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        animalId TEXT NOT NULL,
                        supplierName TEXT NOT NULL,
                        glnNumber TEXT NOT NULL,
                        purchaseDate TEXT NOT NULL,
                        purchaseBatchNumber TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

    /*
     * Version 5 -> 6
     *
     * Adds the Location & Feed table.
     */
    private val MIGRATION_5_6 =
        object : Migration(5, 6) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS location_feed (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        animalId TEXT NOT NULL,
                        destination TEXT NOT NULL,
                        daysInDestination INTEGER NOT NULL,
                        rationName TEXT NOT NULL,
                        rationDays INTEGER NOT NULL,
                        rationCost REAL NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

    /*
     * Version 6 -> 7
     *
     * Adds the Animal Costs table.
     *
     * Used for:
     * - Transport
     * - Processing
     * - Handling
     * - Interest
     */
    private val MIGRATION_6_7 =
        object : Migration(6, 7) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS animal_costs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        animalId TEXT NOT NULL,
                        costType TEXT NOT NULL,
                        amount REAL NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

    /*
     * Version 7 -> 8
     *
     * Adds the database entities merged from main:
     * - Roles
     * - Users
     * - Farmers
     * - Farmer addresses
     * - Farmer roles
     * - Locations
     * - Animals
     * - Animal groups
     */
    private val MIGRATION_7_8 =
        object : Migration(7, 8) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS roles (
                        role_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        role_name TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    index_roles_role_name
                    ON roles(role_name)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                        user_id TEXT NOT NULL PRIMARY KEY,
                        username TEXT NOT NULL,
                        pin_hash TEXT,
                        failed_pin_attempts INTEGER NOT NULL,
                        role INTEGER,
                        device_assigned_id TEXT,
                        device_last_sync INTEGER,
                        failed_sync_attempts INTEGER NOT NULL,
                        FOREIGN KEY(role)
                            REFERENCES roles(role_id)
                            ON UPDATE NO ACTION
                            ON DELETE SET NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    index_users_username
                    ON users(username)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_users_role
                    ON users(role)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS farmers (
                        farmer_id TEXT NOT NULL PRIMARY KEY,
                        client_code TEXT,
                        organisation_name TEXT,
                        vat_number TEXT,
                        email_address TEXT,
                        gps_latitude REAL,
                        gps_longitude REAL,
                        sync_status TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS farmer_addresses (
                        address_id TEXT NOT NULL PRIMARY KEY,
                        farmer_id TEXT NOT NULL,
                        address_type TEXT,
                        address_line_1 TEXT,
                        province TEXT,
                        postal_code TEXT,
                        gps_latitude REAL,
                        gps_longitude REAL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS farmer_roles (
                        farmer_role_id TEXT NOT NULL PRIMARY KEY,
                        farmer_id TEXT NOT NULL,
                        role_id TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS locations (
                        location_id TEXT NOT NULL PRIMARY KEY,
                        location_code TEXT,
                        location_name TEXT,
                        location_type TEXT
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS animal_groups (
                        animalGroupId TEXT NOT NULL PRIMARY KEY,
                        groupName TEXT NOT NULL,
                        description TEXT
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    index_animal_groups_groupName
                    ON animal_groups(groupName)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS animals (
                        animalId TEXT NOT NULL PRIMARY KEY,
                        tagNumber TEXT,
                        oldTagNumber TEXT,
                        temperatureNumber TEXT,
                        referenceNumber TEXT,
                        massKg REAL,
                        birthdate INTEGER NOT NULL,
                        breed TEXT NOT NULL,
                        gender TEXT,
                        age INTEGER,
                        condition TEXT,
                        hideColour TEXT,
                        brandMark TEXT,
                        parentId TEXT,
                        animalGroupId TEXT,
                        photoPath TEXT,
                        videoPath TEXT,
                        gpsLat REAL NOT NULL,
                        gpsLng REAL NOT NULL,
                        captureAt INTEGER NOT NULL,
                        deviceId TEXT NOT NULL,
                        recordguid TEXT NOT NULL,
                        syncStatus TEXT NOT NULL,
                        syncedat INTEGER
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_animals_tagNumber
                    ON animals(tagNumber)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_animals_temperatureNumber
                    ON animals(temperatureNumber)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_animals_parentId
                    ON animals(parentId)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_animals_animalGroupId
                    ON animals(animalGroupId)
                    """.trimIndent()
                )
            }
        }

    /*
     * Version 8 -> 9
     *
     * Adds animal group memberships.
     */
    private val MIGRATION_8_9 =
        object : Migration(8, 9) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS animal_group_memberships (
                        membership_id TEXT NOT NULL PRIMARY KEY,
                        animal_id TEXT NOT NULL,
                        group_id TEXT NOT NULL,
                        joined_at INTEGER NOT NULL,
                        left_at INTEGER,
                        record_guid TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

    fun create(
        context: Context,
        passphrase: ByteArray
    ): DatabaseResult {

        if (passphrase.isEmpty()) {

            return DatabaseResult.Error(
                type = DatabaseErrorType.EMPTY_PASSPHRASE,
                message = "Database passphrase cannot be empty."
            )
        }

        var database: BeefTechDatabase? = null

        return try {

            System.loadLibrary(
                "sqlcipher"
            )

            val factory =
                SupportOpenHelperFactory(
                    passphrase
                )

            database =
                Room.databaseBuilder(
                    context.applicationContext,
                    BeefTechDatabase::class.java,
                    "beeftech.db"
                )
                    .openHelperFactory(
                        factory
                    )

                    /*
                     * Room can now upgrade:
                     *
                     * 1 -> 2
                     * 2 -> 3
                     * 3 -> 4
                     * 4 -> 5
                     * 5 -> 6
                     * 6 -> 7
                     * 7 -> 8
                     * 8 -> 9
                     * 9 -> 10
                     * 10 -> 11
                     * 11 -> 12
                     * 12 -> 13
                     * 13 -> 14
                     */
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        BeefTechDatabase.MIGRATION_9_10,
                        BeefTechDatabase.MIGRATION_10_11,
                        BeefTechDatabase.MIGRATION_11_12,
                        BeefTechDatabase.MIGRATION_12_13,
                        BeefTechDatabase.MIGRATION_13_14
                    )

                    .addCallback(
                        BeefTechDatabase.SEED_CALLBACK
                    )

                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)

                    .build()

            /*
             * Force SQLCipher to open/decrypt
             * the database immediately.
             */
            database.openHelper.writableDatabase

            DatabaseResult.Success(
                database
            )

        } catch (exception: Exception) {

            database?.close()

            val message =
                exception.message
                    ?.lowercase()
                    ?: ""

            when {

                message.contains(
                    "file is not a database"
                ) ||
                        message.contains(
                            "not a database"
                        ) -> {

                    DatabaseResult.Error(
                        type =
                            DatabaseErrorType.INVALID_PASSPHRASE,
                        message =
                            "Unable to unlock the encrypted database.",
                        cause =
                            exception
                    )
                }

                message.contains(
                    "malformed"
                ) ||
                        message.contains(
                            "corrupt"
                        ) -> {

                    DatabaseResult.Error(
                        type =
                            DatabaseErrorType.DATABASE_CORRUPTION,
                        message =
                            "The local database appears to be corrupted.",
                        cause =
                            exception
                    )
                }

                else -> {

                    DatabaseResult.Error(
                        type =
                            DatabaseErrorType.DATABASE_OPEN_ERROR,
                        message =
                            "Unable to open the local database.",
                        cause =
                            exception
                    )
                }
            }
        }
    }
}
