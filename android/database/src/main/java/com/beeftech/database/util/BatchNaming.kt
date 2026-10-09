package com.beeftech.database.util

import com.beeftech.database.security.SyncIdentityRegistry

/**
 * Names a sync upload from the signed-in user's farm code and device.
 *
 * Returns null when either is not known (a device that has not signed in online since farm codes
 * were introduced). The upload then goes without a name, which the server still accepts.
 */
object BatchNaming {

    private class Issued(val name: String, val atMillis: Long)

    /** The newest name given out for each project, so the sync history can say which upload a run sent. */
    private val lastIssued = java.util.concurrent.ConcurrentHashMap<ProjectCode, Issued>()

    fun nameFor(project: ProjectCode, nowMillis: Long = System.currentTimeMillis()): String? {
        val farmCode = SyncIdentityRegistry.farmCode() ?: return null
        val deviceId = SyncIdentityRegistry.deviceId() ?: return null

        return try {
            FileNamingUtils.build(farmCode, project, nowMillis, deviceId).also {
                lastIssued[project] = Issued(it, nowMillis)
            }
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * The newest name given out for [project] at or after [sinceMillis], that is, by the run that
     * started then. Null when that run sent nothing or sent no name.
     */
    fun lastNameSince(project: ProjectCode, sinceMillis: Long): String? =
        lastIssued[project]?.takeIf { it.atMillis >= sinceMillis }?.name

    /** Forgets the names handed out so far. For tests, and when the user signs out. */
    fun forgetIssued() = lastIssued.clear()
}
