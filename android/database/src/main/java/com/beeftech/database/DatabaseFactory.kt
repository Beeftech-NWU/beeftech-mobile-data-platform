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
                     */
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7
                    )

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