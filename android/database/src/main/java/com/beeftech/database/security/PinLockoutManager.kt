
package com.beeftech.database.security

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PinLockoutManager(context: Context)
{

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = try
    {
        EncryptedSharedPreferences.create(
            context,
            "beeftech_sec_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception)
    {
        Log.e("BeefTech_Auth", "KeyStore access error, resetting auth prefs", e)
        context.getSharedPreferences("beeftech_sec_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        null
    }

    companion object
    {
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_TIMESTAMP = "lockout_timestamp"
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 60_000L
    }

    fun recordFailedAttempt(): Int
    {
        val attempts = getFailedAttempts() + 1
        prefs?.edit()?.putInt(KEY_FAILED_ATTEMPTS, attempts)?.apply()

        if (attempts >= MAX_ATTEMPTS)
        {
            prefs?.edit()?.putLong(KEY_LOCKOUT_TIMESTAMP, System.currentTimeMillis())?.apply()
        }
        return attempts
    }

    fun resetAttempts()
    {
        prefs?.edit()
            ?.putInt(KEY_FAILED_ATTEMPTS, 0)
            ?.putLong(KEY_LOCKOUT_TIMESTAMP, 0L)
            ?.apply()
    }

    fun isLockedOut(): Boolean
    {
        if (getFailedAttempts() < MAX_ATTEMPTS) return false

        val lockoutTime = prefs?.getLong(KEY_LOCKOUT_TIMESTAMP, 0L) ?: 0L
        val elapsed = System.currentTimeMillis() - lockoutTime

        return if (elapsed >= LOCKOUT_DURATION_MS)
        {
            resetAttempts()
            false
        } else
        {
            true
        }
    }

    fun getRemainingLockoutTimeMs(): Long
    {
        if (!isLockedOut()) return 0L
        val lockoutTime = prefs?.getLong(KEY_LOCKOUT_TIMESTAMP, 0L) ?: 0L
        val remaining = LOCKOUT_DURATION_MS - (System.currentTimeMillis() - lockoutTime)
        return if (remaining > 0) remaining else 0L
    }

    fun getFailedAttempts(): Int
    {
        return prefs?.getInt(KEY_FAILED_ATTEMPTS, 0) ?: 0
    }
}