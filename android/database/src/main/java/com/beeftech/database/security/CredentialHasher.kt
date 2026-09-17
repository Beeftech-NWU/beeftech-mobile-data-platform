package com.beeftech.database.security

import android.util.Base64
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.MessageDigest
import java.security.SecureRandom

object CredentialHasher {

    private const val SALT_LENGTH = 16
    private const val HASH_LENGTH = 32

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun hash(
        password: CharArray,
        salt: ByteArray
    ): ByteArray {

        val params =
            Argon2Parameters.Builder(
                Argon2Parameters.ARGON2_id
            )
                .withVersion(
                    Argon2Parameters.ARGON2_VERSION_13
                )
                .withIterations(2)
                .withMemoryAsKB(19 * 1024)
                .withParallelism(1)
                .withSalt(salt)
                .build()

        val generator =
            Argon2BytesGenerator()

        generator.init(params)

        val result =
            ByteArray(HASH_LENGTH)

        generator.generateBytes(
            password,
            result,
            0,
            result.size
        )

        return result
    }

    fun verify(
        password: CharArray,
        salt: ByteArray,
        storedHash: ByteArray
    ): Boolean {

        val computedHash =
            hash(
                password,
                salt
            )

        return MessageDigest.isEqual(
            computedHash,
            storedHash
        )
    }

    fun ByteArray.toBase64(): String {
        return Base64.encodeToString(
            this,
            Base64.NO_WRAP
        )
    }

    fun String.fromBase64(): ByteArray {
        return Base64.decode(
            this,
            Base64.NO_WRAP
        )
    }
}