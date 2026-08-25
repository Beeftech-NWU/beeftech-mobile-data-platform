import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object CredentialHasher {
    private const val ITERATIONS = 100000 // Heavy KDF iteration count
    private const val KEY_LENGTH = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    // Generates a unique secure salt for initial setup
    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    // Hashes the password using the generated salt
    fun hashPassword(password: CharArray, salt: ByteArray): String {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hash = factory.generateSecret(spec).encoded

        // Return salt and hash combined for storage
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)
        return "$saltBase64:$hashBase64"
    }

    // Verifies a plaintext password against the stored hash
    fun verifyPasswordHash(password: CharArray, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false

        val salt = Base64.decode(parts[0], Base64.NO_WRAP)
        val expectedHash = parts[1]

        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val testHash = factory.generateSecret(spec).encoded
        val testHashBase64 = Base64.encodeToString(testHash, Base64.NO_WRAP)

        return expectedHash == testHashBase64
    }
}