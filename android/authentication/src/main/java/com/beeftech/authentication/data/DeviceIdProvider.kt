package com.beeftech.authentication.data

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

interface DeviceIdProvider {
    fun getDeviceId(): String
}

class EncryptedDeviceIdProvider(
    context: Context
) : DeviceIdProvider {

    private val appContext =
        context.applicationContext

    private val prefs =
        appContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    override fun getDeviceId(): String {

        /*
         * If a stable BeefTech device ID has already been
         * stored, continue using it.
         */
        prefs.getString(KEY_DEVICE_ID, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        /*
         * ANDROID_ID is stable for this application/device
         * installation and gives us a deterministic source
         * instead of generating a new random UUID.
         */
        val androidId =
            Settings.Secure.getString(
                appContext.contentResolver,
                Settings.Secure.ANDROID_ID
            )
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException(
                    "Unable to determine this device's Android ID."
                )

        /*
         * Do not expose the raw Android ID as the BeefTech
         * device identifier. Hash it and retain the existing
         * MOB_DEV_XXXXXXXX format expected by the backend.
         */
        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest(androidId.toByteArray(Charsets.UTF_8))

        val suffix =
            digest
                .take(4)
                .joinToString("") { byte ->
                    "%02x".format(byte.toInt() and 0xff)
                }

        val deviceId =
            "MOB_DEV_$suffix"

        prefs.edit()
            .putString(KEY_DEVICE_ID, deviceId)
            .commit()

        return deviceId
    }

    companion object {
        private const val PREFS_NAME =
            "beeftech_device_prefs"

        private const val KEY_DEVICE_ID =
            "device_id"
    }
}