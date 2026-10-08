package com.beeftech.database.util

import com.beeftech.database.security.SyncIdentityRegistry

/**
 * Names a sync upload from the signed-in user's farm code and device.
 *
 * Returns null when either is not known (a device that has not signed in online since farm codes
 * were introduced). The upload then goes without a name, which the server still accepts.
 */
object BatchNaming {

    fun nameFor(project: ProjectCode, nowMillis: Long = System.currentTimeMillis()): String? {
        val farmCode = SyncIdentityRegistry.farmCode() ?: return null
        val deviceId = SyncIdentityRegistry.deviceId() ?: return null

        return try {
            FileNamingUtils.build(farmCode, project, nowMillis, deviceId)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
