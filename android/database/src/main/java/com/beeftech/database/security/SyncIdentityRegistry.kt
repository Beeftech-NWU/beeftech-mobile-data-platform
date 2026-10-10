package com.beeftech.database.security

/**
 * The farm code and device id the signed-in user syncs under, shared with the feature modules.
 *
 * The session store sets it on every sign-in and when the app starts, and clears it on logout.
 * Sync clients read it to name each upload batch. It holds nothing secret.
 */
object SyncIdentityRegistry {

    @Volatile
    private var farmCode: String? = null

    @Volatile
    private var deviceId: String? = null

    fun set(farmCode: String?, deviceId: String?) {
        this.farmCode = farmCode?.trim()?.takeIf { it.isNotEmpty() }
        this.deviceId = deviceId?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun farmCode(): String? = farmCode

    fun deviceId(): String? = deviceId

    fun clear() = set(null, null)
}
