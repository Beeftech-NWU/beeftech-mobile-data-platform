package com.beeftech.authentication.data

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.beeftech.authentication.domain.LoggedInUser
import com.beeftech.database.security.CurrentUserIdRegistry
import com.beeftech.database.security.TokenProvider

interface SessionStore : TokenProvider {

    /*
     * Save a real ONLINE session returned by Render.
     *
     * The local application session and the server JWT have the
     * same expiry when we have just authenticated successfully.
     */
    fun save(
        token: String,
        expiresAt: Long,
        user: LoggedInUser
    )

    /*
     * Save an OFFLINE application session.
     *
     * This allows the worker to continue using BeefTech locally
     * without inventing a fake bearer token.
     *
     * A still-valid real Render JWT may be preserved, but an
     * expired/missing/legacy token is removed.
     */
    fun saveOffline(
        expiresAt: Long,
        user: LoggedInUser
    )

    fun currentUser(): LoggedInUser?

    /*
     * Expiry of the LOCAL application session.
     *
     * This is deliberately separate from the Render JWT expiry.
     */
    fun isExpired(
        now: Long = System.currentTimeMillis()
    ): Boolean

    fun clear()
}


class EncryptedSessionStore(
    context: Context
) : SessionStore {

    private val masterKey =
        MasterKey.Builder(context)
            .setKeyScheme(
                MasterKey.KeyScheme.AES256_GCM
            )
            .build()

    private val prefs =
        try {

            EncryptedSharedPreferences.create(
                context,
                "beeftech_session_prefs",
                masterKey,
                EncryptedSharedPreferences
                    .PrefKeyEncryptionScheme
                    .AES256_SIV,
                EncryptedSharedPreferences
                    .PrefValueEncryptionScheme
                    .AES256_GCM
            )

        } catch (e: Exception) {

            Log.e(
                "BeefTech_Auth",
                "KeyStore access error, resetting session prefs",
                e
            )

            context
                .getSharedPreferences(
                    "beeftech_session_prefs",
                    Context.MODE_PRIVATE
                )
                .edit()
                .clear()
                .apply()

            null
        }


    companion object {

        /*
         * Real JWT supplied by Render.
         */
        private const val KEY_TOKEN =
            "token"

        /*
         * LOCAL application session expiry.
         *
         * This may be up to seven days for offline use.
         */
        private const val KEY_EXPIRES_AT =
            "expires_at"

        /*
         * Expiry of the actual Render JWT.
         *
         * Never extend this merely because the user logged in
         * offline.
         */
        private const val KEY_SERVER_TOKEN_EXPIRES_AT =
            "server_token_expires_at"

        private const val KEY_USER_ID =
            "user_id"

        private const val KEY_USERNAME =
            "username"

        private const val KEY_ROLE =
            "role"

        private const val KEY_DEVICE_ID =
            "device_id"

        private const val KEY_SITE_ID =
            "site_id"
    }


    /*
     * Successful ONLINE login.
     */
    override fun save(
        token: String,
        expiresAt: Long,
        user: LoggedInUser
    ) {

        prefs
            ?.edit()
            ?.putString(
                KEY_TOKEN,
                token
            )
            ?.putLong(
                KEY_EXPIRES_AT,
                expiresAt
            )
            ?.putLong(
                KEY_SERVER_TOKEN_EXPIRES_AT,
                expiresAt
            )
            ?.putString(
                KEY_USER_ID,
                user.userId
            )
            ?.putString(
                KEY_USERNAME,
                user.username
            )
            ?.putInt(
                KEY_ROLE,
                user.role ?: -1
            )
            ?.putString(
                KEY_DEVICE_ID,
                user.deviceId
            )
            ?.putString(
                KEY_SITE_ID,
                user.siteId
            )
            ?.apply()

        CurrentUserIdRegistry
            .setCurrentUserId(
                if (prefs != null) {
                    user.userId
                } else {
                    null
                }
            )
    }


    /*
     * Successful OFFLINE login.
     *
     * Local access is extended, but server credentials are not.
     */
    override fun saveOffline(
        expiresAt: Long,
        user: LoggedInUser
    ) {

        val now =
            System.currentTimeMillis()

        val storedUserId =
            prefs
                ?.getString(
                    KEY_USER_ID,
                    null
                )

        val storedToken =
            prefs
                ?.getString(
                    KEY_TOKEN,
                    null
                )
                ?.trim()

        val serverTokenExpiresAt =
            prefs
                ?.getLong(
                    KEY_SERVER_TOKEN_EXPIRES_AT,
                    0L
                )
                ?: 0L

        /*
         * Keep a server JWT only when:
         *
         * 1. It belongs to the same user.
         * 2. It is a real non-empty token.
         * 3. It is not one of the old OFFLINE_TOKEN values.
         * 4. Its real server expiry has not passed.
         *
         * Old installations do not have
         * KEY_SERVER_TOKEN_EXPIRES_AT, therefore their old token
         * is deliberately discarded and one online login is
         * required.
         */
        val keepServerToken =
            storedUserId == user.userId &&
                !storedToken.isNullOrBlank() &&
                !storedToken.startsWith(
                    "OFFLINE_TOKEN_"
                ) &&
                serverTokenExpiresAt > now

        val editor =
            prefs?.edit()

        if (!keepServerToken) {

            editor
                ?.remove(
                    KEY_TOKEN
                )
                ?.remove(
                    KEY_SERVER_TOKEN_EXPIRES_AT
                )
        }

        editor
            ?.putLong(
                KEY_EXPIRES_AT,
                expiresAt
            )
            ?.putString(
                KEY_USER_ID,
                user.userId
            )
            ?.putString(
                KEY_USERNAME,
                user.username
            )
            ?.putInt(
                KEY_ROLE,
                user.role ?: -1
            )
            ?.putString(
                KEY_DEVICE_ID,
                user.deviceId
            )
            ?.putString(
                KEY_SITE_ID,
                user.siteId
            )
            ?.apply()

        CurrentUserIdRegistry
            .setCurrentUserId(
                if (prefs != null) {
                    user.userId
                } else {
                    null
                }
            )
    }


    override fun currentUser():
            LoggedInUser? {

        val userId =
            prefs
                ?.getString(
                    KEY_USER_ID,
                    null
                )
                ?: return null

        val username =
            prefs
                ?.getString(
                    KEY_USERNAME,
                    null
                )
                ?: return null

        val deviceId =
            prefs
                ?.getString(
                    KEY_DEVICE_ID,
                    ""
                )
                ?: ""

        val siteId =
            prefs
                ?.getString(
                    KEY_SITE_ID,
                    null
                )

        val roleValue =
            prefs
                ?.getInt(
                    KEY_ROLE,
                    -1
                )
                ?: -1

        val role =
            if (roleValue == -1) {
                null
            } else {
                roleValue
            }

        return LoggedInUser(
            userId = userId,
            username = username,
            role = role,
            deviceId = deviceId,
            siteId = siteId
        )
    }


    /*
     * LOCAL session expiry.
     */
    override fun isExpired(
        now: Long
    ): Boolean {

        val expiresAt =
            prefs
                ?.getLong(
                    KEY_EXPIRES_AT,
                    0L
                )
                ?: 0L

        return now >= expiresAt
    }


    override fun clear() {

        prefs
            ?.edit()
            ?.clear()
            ?.apply()

        CurrentUserIdRegistry
            .setCurrentUserId(
                null
            )
    }


    /*
     * TokenProvider used by WorkManager/API clients.
     *
     * Only return a token that is known to still be a valid
     * server-side session candidate.
     */
    override suspend fun token():
            String? {

        /*
         * If even the local app session expired, there is no
         * usable server session either.
         */
        if (isExpired()) {
            return null
        }

        val serverTokenExpiresAt =
            prefs
                ?.getLong(
                    KEY_SERVER_TOKEN_EXPIRES_AT,
                    0L
                )
                ?: 0L

        /*
         * Do NOT use the seven-day offline expiry to decide
         * whether the Render JWT is valid.
         */
        if (
            System.currentTimeMillis() >=
            serverTokenExpiresAt
        ) {
            return null
        }

        val token =
            prefs
                ?.getString(
                    KEY_TOKEN,
                    null
                )
                ?.trim()

        if (token.isNullOrEmpty()) {
            return null
        }

        /*
         * Migration protection for sessions created by the old
         * implementation.
         */
        if (
            token.startsWith(
                "OFFLINE_TOKEN_"
            )
        ) {
            return null
        }

        return token
    }
}
