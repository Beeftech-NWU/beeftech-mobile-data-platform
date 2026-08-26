package com.beeftech.database

import com.beeftech.database.security.CredentialHasher
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialHasherTest {

    @Test
    fun generateSalt_returns16Bytes() {
        val salt = CredentialHasher.generateSalt()

        assertEquals(16, salt.size)
    }

    @Test
    fun generateSalt_generatesDifferentSalts() {
        val salt1 = CredentialHasher.generateSalt()
        val salt2 = CredentialHasher.generateSalt()

        assertFalse(salt1.contentEquals(salt2))
    }

    @Test
    fun hash_returns32Bytes() {
        val password = "1234".toCharArray()
        val salt = CredentialHasher.generateSalt()

        val hash = CredentialHasher.hash(password, salt)

        assertEquals(32, hash.size)
    }

    @Test
    fun samePasswordAndSalt_produceSameHash() {
        val password = "1234".toCharArray()
        val salt = CredentialHasher.generateSalt()

        val hash1 = CredentialHasher.hash(password, salt)
        val hash2 = CredentialHasher.hash(password, salt)

        assertArrayEquals(hash1, hash2)
    }

    @Test
    fun verify_correctPassword_returnsTrue() {
        val salt = CredentialHasher.generateSalt()
        val storedHash = CredentialHasher.hash(
            "1234".toCharArray(),
            salt
        )

        val result = CredentialHasher.verify(
            "1234".toCharArray(),
            salt,
            storedHash
        )

        assertTrue(result)
    }

    @Test
    fun verify_wrongPassword_returnsFalse() {
        val salt = CredentialHasher.generateSalt()
        val storedHash = CredentialHasher.hash(
            "1234".toCharArray(),
            salt
        )

        val result = CredentialHasher.verify(
            "9999".toCharArray(),
            salt,
            storedHash
        )

        assertFalse(result)
    }

    @Test
    fun base64Conversion_returnsOriginalBytes() {
        val original = CredentialHasher.generateSalt()

        val encoded = with(CredentialHasher) {
            original.toBase64()
        }

        val decoded = with(CredentialHasher) {
            encoded.fromBase64()
        }

        assertArrayEquals(original, decoded)
    }
}