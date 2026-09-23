package com.beeftech.authentication.data

import android.content.Context
import java.util.UUID

interface DeviceIdProvider {
    fun getDeviceId(): String
}

class EncryptedDeviceIdProvider(context: Context) : DeviceIdProvider {

    private val prefs = context.getSharedPreferences("beeftech_device_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DEVICE_ID = "device_id"
    }

    override fun getDeviceId(): String {
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id == null) {
            id = "MOB_DEV_" + UUID.randomUUID().toString().take(8)
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }
}
