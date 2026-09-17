package com.beeftech.database.security

import android.content.Context
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

class DatabaseKeyProvider(
    context: Context
) : DatabaseSecurityProvider {

    private val appContext = context.applicationContext

    private val preferences = appContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    private var cachedPassphrase: ByteArray? = null

    override fun initializeKey(passcode: String) {

        validatePasscode(passcode)

        synchronized(this) {

            if (isKeyAvailable()) {
                // Verify that the supplied passcode can unlock
                // the already existing database key.
                val passphrase = decryptDatabasePassphrase(passcode)

                clearCachedPassphrase()

                cachedPassphrase = passphrase.copyOf()

                passphrase.fill(0)

                return
            }

            try {
                val masterKey = getOrCreateMasterKey()

                val databasePassphrase = ByteArray(DATABASE_KEY_SIZE)
                SecureRandom().nextBytes(databasePassphrase)

                val cipher = Cipher.getInstance(TRANSFORMATION)

                cipher.init(
                    Cipher.ENCRYPT_MODE,
                    masterKey
                )

                cipher.updateAAD(
                    passcode.toByteArray(Charsets.UTF_8)
                )

                val encryptedPassphrase =
                    cipher.doFinal(databasePassphrase)

                val iv = cipher.iv

                preferences.edit()
                    .putString(
                        PREF_ENCRYPTED_DATABASE_KEY,
                        Base64.encodeToString(
                            encryptedPassphrase,
                            Base64.NO_WRAP
                        )
                    )
                    .putString(
                        PREF_DATABASE_KEY_IV,
                        Base64.encodeToString(
                            iv,
                            Base64.NO_WRAP
                        )
                    )
                    .apply()

                clearCachedPassphrase()

                cachedPassphrase =
                    databasePassphrase.copyOf()

                databasePassphrase.fill(0)

            } catch (exception: Exception) {

                throw DatabaseSecurityException(
                    "Unable to initialise the database encryption key.",
                    exception
                )
            }
        }
    }

    override fun getDatabasePassphrase(
        passcode: String
    ): ByteArray {

        validatePasscode(passcode)

        synchronized(this) {

            cachedPassphrase?.let {
                return it.copyOf()
            }

            val passphrase =
                decryptDatabasePassphrase(passcode)

            cachedPassphrase =
                passphrase.copyOf()

            return passphrase
        }
    }

    override fun isKeyAvailable(): Boolean {

        val encryptedKey =
            preferences.getString(
                PREF_ENCRYPTED_DATABASE_KEY,
                null
            )

        val iv =
            preferences.getString(
                PREF_DATABASE_KEY_IV,
                null
            )

        return encryptedKey != null &&
                iv != null &&
                keyStoreContainsAlias()
    }

    override fun clearKeyFromMemory() {

        synchronized(this) {
            clearCachedPassphrase()
        }
    }

    override fun invalidateDatabaseKey() {

        synchronized(this) {

            try {
                clearCachedPassphrase()

                preferences.edit()
                    .remove(PREF_ENCRYPTED_DATABASE_KEY)
                    .remove(PREF_DATABASE_KEY_IV)
                    .apply()

                val keyStore = getKeyStore()

                if (keyStore.containsAlias(KEY_ALIAS)) {
                    keyStore.deleteEntry(KEY_ALIAS)
                }

            } catch (exception: Exception) {

                throw DatabaseSecurityException(
                    "Unable to invalidate the database encryption key.",
                    exception
                )
            }
        }
    }

    private fun decryptDatabasePassphrase(
        passcode: String
    ): ByteArray {

        if (!isKeyAvailable()) {
            throw DatabaseSecurityException(
                "Database encryption key has not been initialised."
            )
        }

        try {
            val encryptedKeyBase64 =
                preferences.getString(
                    PREF_ENCRYPTED_DATABASE_KEY,
                    null
                ) ?: throw DatabaseSecurityException(
                    "Encrypted database key is missing."
                )

            val ivBase64 =
                preferences.getString(
                    PREF_DATABASE_KEY_IV,
                    null
                ) ?: throw DatabaseSecurityException(
                    "Database key IV is missing."
                )

            val encryptedKey =
                Base64.decode(
                    encryptedKeyBase64,
                    Base64.NO_WRAP
                )

            val iv =
                Base64.decode(
                    ivBase64,
                    Base64.NO_WRAP
                )

            val masterKey =
                getExistingMasterKey()

            val cipher =
                Cipher.getInstance(TRANSFORMATION)

            cipher.init(
                Cipher.DECRYPT_MODE,
                masterKey,
                GCMParameterSpec(
                    GCM_TAG_LENGTH_BITS,
                    iv
                )
            )

            cipher.updateAAD(
                passcode.toByteArray(Charsets.UTF_8)
            )

            return cipher.doFinal(encryptedKey)

        } catch (exception: AEADBadTagException) {

            throw DatabaseSecurityException(
                "The supplied passcode is incorrect.",
                exception
            )

        } catch (exception: DatabaseSecurityException) {

            throw exception

        } catch (exception: Exception) {

            throw DatabaseSecurityException(
                "Unable to retrieve the database passphrase.",
                exception
            )
        }
    }

    private fun getOrCreateMasterKey(): SecretKey {

        val keyStore = getKeyStore()

        val existingKey =
            keyStore.getKey(
                KEY_ALIAS,
                null
            ) as? SecretKey

        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keySpecification =
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setKeySize(256)
                .build()

        keyGenerator.init(keySpecification)

        return keyGenerator.generateKey()
    }

    private fun getExistingMasterKey(): SecretKey {

        val keyStore = getKeyStore()

        return keyStore.getKey(
            KEY_ALIAS,
            null
        ) as? SecretKey
            ?: throw DatabaseSecurityException(
                "Database master key is not available."
            )
    }

    private fun getKeyStore(): KeyStore {

        return KeyStore.getInstance(
            ANDROID_KEYSTORE
        ).apply {
            load(null)
        }
    }

    private fun keyStoreContainsAlias(): Boolean {

        return try {
            getKeyStore().containsAlias(KEY_ALIAS)
        } catch (_: Exception) {
            false
        }
    }

    private fun validatePasscode(
        passcode: String
    ) {

        if (passcode.isBlank()) {
            throw DatabaseSecurityException(
                "Database passcode cannot be empty."
            )
        }
    }

    private fun clearCachedPassphrase() {

        cachedPassphrase?.fill(0)
        cachedPassphrase = null
    }

    companion object {

        private const val ANDROID_KEYSTORE =
            "AndroidKeyStore"

        private const val KEY_ALIAS =
            "beeftech_database_master_key"

        private const val TRANSFORMATION =
            "AES/GCM/NoPadding"

        private const val GCM_TAG_LENGTH_BITS =
            128

        private const val DATABASE_KEY_SIZE =
            32

        private const val PREFERENCES_NAME =
            "beeftech_database_security"

        private const val PREF_ENCRYPTED_DATABASE_KEY =
            "encrypted_database_key"

        private const val PREF_DATABASE_KEY_IV =
            "database_key_iv"
    }
}