package com.beeftech.management.data

/*
 * The same rules the server checks, so the form can say what's wrong before sending. The server
 * stays the authority: a set it refuses comes back as a message.
 */
object SyncPolicyRules {

    /* The day unsynced data is wiped. Shown to the admin, never editable. */
    const val WIPE_DAY = 7

    const val MIN_WARNING_DAY = 1
    const val MAX_WARNING_DAY = WIPE_DAY - 1
    const val MIN_STALE_HOURS = 12
    const val MAX_STALE_HOURS = 336
    const val WARNING_COUNT = 3

    /* Null if [days] is three whole days, each from 1 to 6, getting larger. */
    fun warningDaysError(days: List<Int?>): String? =
        when {
            days.size != WARNING_COUNT || days.any { it == null } -> "Enter all three warning days"
            days.any { it!! !in MIN_WARNING_DAY..MAX_WARNING_DAY } ->
                "Warning days must be from $MIN_WARNING_DAY to $MAX_WARNING_DAY. Data is wiped on day $WIPE_DAY."
            days.zipWithNext().any { (a, b) -> a!! >= b!! } -> "Warning days must increase, for example 2, 4, 6"
            else -> null
        }

    fun staleHoursError(hours: Int?): String? =
        when {
            hours == null -> "Enter the stale-sync hours"
            hours !in MIN_STALE_HOURS..MAX_STALE_HOURS -> "Hours must be from $MIN_STALE_HOURS to $MAX_STALE_HOURS"
            else -> null
        }
}
