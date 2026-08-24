package com.beeftech.database

import android.content.Context
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object DatabaseFactory {

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

            System.loadLibrary("sqlcipher")

            val factory =
                SupportOpenHelperFactory(passphrase)

            database =
                Room.databaseBuilder(
                    context.applicationContext,
                    BeefTechDatabase::class.java,
                    "beeftech.db"
                )
                    .openHelperFactory(factory)
                    .build()

            // Force SQLCipher to actually open/decrypt the database now.
            database.openHelper.writableDatabase

            DatabaseResult.Success(database)

        } catch (exception: Exception) {

            database?.close()

            val message =
                exception.message?.lowercase() ?: ""

            when {

                message.contains("file is not a database") ||
                        message.contains("not a database") -> {

                    DatabaseResult.Error(
                        type = DatabaseErrorType.INVALID_PASSPHRASE,
                        message = "Unable to unlock the encrypted database.",
                        cause = exception
                    )
                }

                message.contains("malformed") ||
                        message.contains("corrupt") -> {

                    DatabaseResult.Error(
                        type = DatabaseErrorType.DATABASE_CORRUPTION,
                        message = "The local database appears to be corrupted.",
                        cause = exception
                    )
                }

                else -> {

                    DatabaseResult.Error(
                        type = DatabaseErrorType.DATABASE_OPEN_ERROR,
                        message = "Unable to open the local database.",
                        cause = exception
                    )
                }
            }
        }
    }
}