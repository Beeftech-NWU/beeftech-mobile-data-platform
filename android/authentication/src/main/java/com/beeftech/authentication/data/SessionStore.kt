package com.beeftech.authentication.data

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.beeftech.authentication.domain.LoggedInUser
import com.beeftech.database.security.TokenProvider

interface SessionStore : TokenProvider {
    fun save(token: String, expiresAt: Long, user: LoggedInUser)
    fun currentUser(): LoggedInUser?
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean
    fun clear()
}

class EncryptedSessionStore(context: Context) : SessionStore {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = try {
        EncryptedSharedPreferences.create(
            context,
            "beeftech_session_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("BeefTech_Auth", "KeyStore access error, resetting session prefs", e)
        context.getSharedPreferences("beeftech_session_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
        null
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_ROLE = "role"
        private const val KEY_DEVICE_ID = "device_id"
    }

    override fun save(token: String, expiresAt: Long, user: LoggedInUser) {
        prefs?.edit()
            ?.putString(KEY_TOKEN, token)
            ?.putLong(KEY_EXPIRES_AT, expiresAt)
            ?.putString(KEY_USER_ID, user.userId)
            ?.putString(KEY_USERNAME, user.username)
            ?.putInt(KEY_ROLE, user.role ?: -1)
            ?.putString(KEY_DEVICE_ID, user.deviceId)
            ?.apply()
    }

    override fun currentUser(): LoggedInUser? {
        val userId = prefs?.getString(KEY_USER_ID, null) ?: return null
        val username = prefs?.getString(KEY_USERNAME, null) ?: return null
        val deviceId = prefs?.getString(KEY_DEVICE_ID, "") ?: ""
        val roleVal = prefs?.getInt(KEY_ROLE, -1) ?: -1
        val role = if (roleVal == -1) null else roleVal

        return LoggedInUser(
            userId = userId,
            username = username,
            role = role,
            deviceId = deviceId
        )
    }

    override fun isExpired(now: Long): Boolean {
        val expiresAt = prefs?.getLong(KEY_EXPIRES_AT, 0L) ?: 0L
        return now >= expiresAt
    }

    override fun clear() {
        prefs?.edit()?.clear()?.apply()
    }

    override suspend fun token(): String? {
        if (isExpired()) {
            return null
        }
        return prefs?.getString(KEY_TOKEN, null)
    }
}
