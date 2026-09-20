package com.beeftech.authentication.data

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.beeftech.authentication.data.dto.UserProfileDto
import com.beeftech.authentication.domain.TokenProvider

/**
 * Holds the logged-in session (token, expiry, and cached profile fields)
 * in EncryptedSharedPreferences, matching the same key-derivation approach
 * PinLockoutManager already uses in this codebase. Implements
 * [TokenProvider] directly so feature modules (e.g. CalfRegistrationApiClient)
 * can depend on the one-method interface without seeing this class.
 */
class SessionStore(context: Context) : TokenProvider {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = try {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("BeefTech_Auth", "KeyStore access error, resetting session prefs", e)

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()

        null
    }

    override suspend fun token(): String? {
        return prefs?.getString(KEY_TOKEN, null)
    }

    fun saveSession(
        token: String,
        expiresAt: String,
        user: UserProfileDto
    ) {
        prefs?.edit()
            ?.putString(KEY_TOKEN, token)
            ?.putString(KEY_EXPIRES_AT, expiresAt)
            ?.putString(KEY_USER_ID, user.userId)
            ?.putString(KEY_USERNAME, user.username)
            ?.putLong(KEY_ROLE, user.role)
            ?.putString(KEY_DEVICE_ID, user.deviceAssignedId)
            ?.apply()
    }

    fun getUserId(): String? = prefs?.getString(KEY_USER_ID, null)
    fun getUsername(): String? = prefs?.getString(KEY_USERNAME, null)
    fun getRole(): Long? = prefs?.getLong(KEY_ROLE, -1L)?.takeIf { it >= 0L }
    fun getDeviceId(): String? = prefs?.getString(KEY_DEVICE_ID, null)
    fun getExpiresAt(): String? = prefs?.getString(KEY_EXPIRES_AT, null)

    fun hasSession(): Boolean = prefs?.getString(KEY_TOKEN, null) != null

    fun clear() {
        prefs?.edit()?.clear()?.apply()
    }

    /**
     * Rebuilds the session's profile fields from a cached [User] row during
     * an offline login, WITHOUT touching the existing token/expiry - per
     * the login plan, a successful offline login leaves the token as-is
     * (it may be stale; sync will wait for the next real online login).
     */
    fun saveOfflineSession(user: com.beeftech.database.entity.User) {
        prefs?.edit()
            ?.putString(KEY_USER_ID, user.userId)
            ?.putString(KEY_USERNAME, user.username)
            ?.putLong(KEY_ROLE, user.role ?: -1L)
            ?.putString(KEY_DEVICE_ID, user.deviceAssignedId)
            ?.apply()
    }

    companion object {
        private const val PREFS_NAME = "beeftech_session_prefs"
        private const val KEY_TOKEN = "token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_ROLE = "role"
        private const val KEY_DEVICE_ID = "device_id"
    }
}

