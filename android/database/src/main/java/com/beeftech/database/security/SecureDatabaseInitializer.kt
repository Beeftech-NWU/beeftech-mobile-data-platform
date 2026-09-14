package com.beeftech.database.security

import android.content.Context
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.DatabaseResult

object SecureDatabaseInitializer {

    fun initialize(
        context: Context,
        passcode: String
    ): DatabaseResult {

        val securityProvider =
            DatabaseKeyProvider(context)

        try {

            if (!securityProvider.isKeyAvailable()) {
                securityProvider.initializeKey(passcode)
            }

            val passphrase =
                securityProvider.getDatabasePassphrase(passcode)

            return try {

                DatabaseProvider.initialize(
                    context = context.applicationContext,
                    passphrase = passphrase
                )

            } finally {
                // Remove our local copy from memory.
                passphrase.fill(0)
            }

        } finally {
            // Remove the provider's cached copy.
            securityProvider.clearKeyFromMemory()
        }
    }
}