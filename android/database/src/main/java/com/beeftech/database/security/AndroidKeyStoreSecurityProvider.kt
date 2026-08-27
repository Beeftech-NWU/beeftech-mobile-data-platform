package com.beeftech.database.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidKeyStoreSecurityProvider(
    context: Context
) : DatabaseSecurityProvider {

    companion object {

        private const val ANDROID_KEYSTORE =
            "AndroidKeyStore"

        private const val DATABASE_MASTER_KEY_ALIAS =
            "beeftech_database_master_key"

        private const val TRANSFORMATION =
            "AES/GCM/NoPadding"

        private const val GCM_TAG_LENGTH_BITS =
            128

        private const val DATABASE_PASSPHRASE_LENGTH =
            32

        private const val MOCK_PASSCODE =
            "DEV_ONLY_123456"
    }

    private val keyStore: KeyStore =
        KeyStore.getInstance(
            ANDROID_KEYSTORE
        ).apply {
            load(null)
        }

    private val passphraseStore =
        SecureDatabasePassphraseStore(
            context.applicationContext
        )

    private var cachedPassphrase: ByteArray? = null

    override fun initializeKey(
        passcode: String
    ) {

        validatePasscode(passcode)

        try {

            if (!isKeyAvailable()) {
                generateDatabaseMasterKey()
            }

            if (!passphraseStore.containsPassphrase()) {
                createAndStoreDatabasePassphrase()
            }

        } catch (exception: GeneralSecurityException) {

            throw DatabaseSecurityException(
                "Unable to initialize database security.",
                exception
            )
        }
    }

    override fun getDatabasePassphrase(
        passcode: String
    ): ByteArray {

        validatePasscode(passcode)

        if (!isKeyAvailable()) {
            throw DatabaseSecurityException(
                "Database master key is unavailable."
            )
        }

        val encryptedPassphrase =
            passphraseStore.getEncryptedPassphrase()
                ?: throw DatabaseSecurityException(
                    "Encrypted database passphrase is unavailable."
                )

        val iv =
            passphraseStore.getInitializationVector()
                ?: throw DatabaseSecurityException(
                    "Database passphrase IV is unavailable."
                )

        return try {

            val masterKey = getMasterKey()

            val cipher = Cipher.getInstance(
                TRANSFORMATION
            )

            val gcmParameterSpec =
                GCMParameterSpec(
                    GCM_TAG_LENGTH_BITS,
                    iv
                )

            cipher.init(
                Cipher.DECRYPT_MODE,
                masterKey,
                gcmParameterSpec
            )

            val decryptedPassphrase =
                cipher.doFinal(
                    encryptedPassphrase
                )

            cachedPassphrase?.fill(0)

            cachedPassphrase =
                decryptedPassphrase.copyOf()

            decryptedPassphrase

        } catch (exception: GeneralSecurityException) {

            throw DatabaseSecurityException(
                "Unable to decrypt database passphrase.",
                exception
            )
        } finally {

            encryptedPassphrase.fill(0)
            iv.fill(0)
        }
    }

    override fun isKeyAvailable(): Boolean {
        return try {

            keyStore.containsAlias(
                DATABASE_MASTER_KEY_ALIAS
            )

        } catch (exception: Exception) {

            false
        }
    }

    override fun clearKeyFromMemory() {

        cachedPassphrase?.fill(0)

        cachedPassphrase = null
    }

    override fun invalidateDatabaseKey() {

        clearKeyFromMemory()

        try {

            passphraseStore.clear()

            if (isKeyAvailable()) {

                keyStore.deleteEntry(
                    DATABASE_MASTER_KEY_ALIAS
                )
            }

        } catch (exception: Exception) {

            throw DatabaseSecurityException(
                "Unable to invalidate database security.",
                exception
            )
        }
    }

    private fun generateDatabaseMasterKey() {

        val keyGenerator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

        val keySpec =
            KeyGenParameterSpec.Builder(
                DATABASE_MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .build()

        keyGenerator.init(keySpec)

        keyGenerator.generateKey()
    }

    private fun createAndStoreDatabasePassphrase() {

        val databasePassphrase =
            ByteArray(
                DATABASE_PASSPHRASE_LENGTH
            )

        try {

            SecureRandom().nextBytes(
                databasePassphrase
            )

            val masterKey =
                getMasterKey()

            val cipher =
                Cipher.getInstance(
                    TRANSFORMATION
                )

            cipher.init(
                Cipher.ENCRYPT_MODE,
                masterKey
            )

            val encryptedPassphrase =
                cipher.doFinal(
                    databasePassphrase
                )

            val iv =
                cipher.iv.copyOf()

            try {

                passphraseStore
                    .saveEncryptedPassphrase(
                        ciphertext =
                            encryptedPassphrase,
                        iv = iv
                    )

            } finally {

                encryptedPassphrase.fill(0)
                iv.fill(0)
            }

        } catch (exception: GeneralSecurityException) {

            throw DatabaseSecurityException(
                "Unable to create database passphrase.",
                exception
            )

        } finally {

            databasePassphrase.fill(0)
        }
    }

    private fun getMasterKey(): SecretKey {

        val entry =
            keyStore.getEntry(
                DATABASE_MASTER_KEY_ALIAS,
                null
            ) as? KeyStore.SecretKeyEntry
                ?: throw DatabaseSecurityException(
                    "Database master key was not found."
                )

        return entry.secretKey
    }

    private fun validatePasscode(
        passcode: String
    ) {

        if (passcode != MOCK_PASSCODE) {

            throw DatabaseSecurityException(
                "Database authentication failed."
            )
        }
    }
}
