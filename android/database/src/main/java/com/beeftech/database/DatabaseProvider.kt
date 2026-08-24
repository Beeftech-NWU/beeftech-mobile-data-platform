package com.beeftech.database

import android.content.Context

object DatabaseProvider {

    @Volatile
    private var database: BeefTechDatabase? = null

    fun initialize(
        context: Context,
        passphrase: ByteArray
    ): DatabaseResult {

        database?.let {
            return DatabaseResult.Success(it)
        }

        synchronized(this) {

            database?.let {
                return DatabaseResult.Success(it)
            }

            val result = DatabaseFactory.create(
                context = context,
                passphrase = passphrase
            )

            if (result is DatabaseResult.Success) {
                database = result.database
            }

            return result
        }
    }

    fun getDatabase(): BeefTechDatabase? {
        return database
    }

    fun isOpen(): Boolean {
        return database?.isOpen == true
    }

    fun close() {
        database?.close()
        database = null
    }
}