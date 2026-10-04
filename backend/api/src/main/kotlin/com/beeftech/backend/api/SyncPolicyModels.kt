package com.beeftech.backend.api

import kotlinx.serialization.Serializable

/*
 * Server-side defaults and limits for the sync policy.
 *
 * The wipe day is NOT a setting: the phone wipes unsynced data at day 7 whatever the server
 * says, and an admin can only move the warnings before it. That way a mistake on the server
 * can never cause a wipe earlier than the app already guarantees.
 */
object SyncPolicyLimits {
    const val WIPE_DAY = 7
    const val WARNING_COUNT = 3
    const val MIN_WARNING_DAY = 1
    const val MAX_WARNING_DAY = WIPE_DAY - 1
    const val MIN_STALE_HOURS = 12
    const val MAX_STALE_HOURS = 336
    val DEFAULT_WARNING_DAYS = listOf(2, 4, 6)
    const val DEFAULT_STALE_HOURS = 48
}

/* No default values here: the server leaves defaults out of its JSON, and the app needs every field. */
@Serializable
data class SyncPolicyDto(
    val version: Long,
    val warningDays: List<Int>,
    /* Informational. Fixed in the app, and not changeable here. */
    val wipeDay: Int,
    val staleSyncAlertHours: Int
)

@Serializable
data class UpdateSyncPolicyRequest(
    val warningDays: List<Int>,
    val staleSyncAlertHours: Int
)
