package com.beeftech.database.security

import android.content.Context
import android.util.Base64

class SecureDatabasePassphraseStore(
    context: Context
) {

    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun saveEncryptedPassphrase(
        ciphertext: ByteArray,
        iv: ByteArray
    ) {
        preferences.edit()
            .putString(
                CIPHERTEXT_KEY,
                ciphertext.toBase64()
            )
            .putString(
                IV_KEY,
                iv.toBase64()
            )
            .commit()
    }

    fun getEncryptedPassphrase(): ByteArray? {
        return preferences
            .getString(CIPHERTEXT_KEY, null)
            ?.fromBase64()
    }

    fun getInitializationVector(): ByteArray? {
        return preferences
            .getString(IV_KEY, null)
            ?.fromBase64()
    }

    fun containsPassphrase(): Boolean {
        return preferences.contains(CIPHERTEXT_KEY) &&
                preferences.contains(IV_KEY)
    }

    fun clear() {
        preferences.edit()
            .remove(CIPHERTEXT_KEY)
            .remove(IV_KEY)
            .commit()
    }

    companion object {

        private const val PREFERENCES_NAME =
            "beeftech_database_security"

        private const val CIPHERTEXT_KEY =
            "encrypted_database_passphrase"

        private const val IV_KEY =
            "database_passphrase_iv"
    }
}

private fun ByteArray.toBase64(): String {
    return Base64.encodeToString(
        this,
        Base64.NO_WRAP
    )
}

private fun String.fromBase64(): ByteArray {
    return Base64.decode(
        this,
        Base64.NO_WRAP
    )
}
