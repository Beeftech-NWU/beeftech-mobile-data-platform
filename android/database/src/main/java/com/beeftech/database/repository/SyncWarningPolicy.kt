package com.beeftech.database.repository

/**
 * When the app warns about unsynced data, and the one day it never moves: the wipe.
 *
 * The server can move the three warnings (see [sanitize]), but [WIPE_DAY] is a constant that no
 * server value is ever read into. A bad or malicious value can therefore delay or add a warning,
 * and can never make the app wipe earlier (or later) than day 7.
 *
 * Levels: 0 none, 1 to 3 the three warnings, 4 the wipe day (locked).
 */
class SyncWarningPolicy private constructor(
    /* Always three days, strictly increasing, each from 1 to 6. */
    val warningDays: List<Int>
) {

    fun levelFor(ageDays: Long): Int =
        if (ageDays >= WIPE_DAY) {
            WIPE_LEVEL
        } else {
            warningDays.count { ageDays >= it }
        }

    /* "2,4,6": how the policy is stored in device_config. */
    fun serialize(): String = warningDays.joinToString(",")

    override fun equals(other: Any?): Boolean =
        other is SyncWarningPolicy && other.warningDays == warningDays

    override fun hashCode(): Int = warningDays.hashCode()

    override fun toString(): String = "SyncWarningPolicy($warningDays)"

    companion object {

        /* The day unsynced data is wiped and the account locked. Not configurable. */
        const val WIPE_DAY = 7

        const val WIPE_LEVEL = 4

        private const val WARNING_COUNT = 3
        private const val MIN_DAY = 1
        private const val MAX_DAY = WIPE_DAY - 1

        val DEFAULT: SyncWarningPolicy = SyncWarningPolicy(listOf(2, 4, 6))

        /*
         * Keeps [days] only if it is exactly three strictly increasing days from 1 to 6.
         * Anything else (a wrong count, an out-of-range or repeated day, null) gives the default,
         * so a bad value from the server can never reach the enforcer.
         */
        fun sanitize(days: List<Int>?): SyncWarningPolicy {
            if (days == null || days.size != WARNING_COUNT) return DEFAULT
            if (days.any { it !in MIN_DAY..MAX_DAY }) return DEFAULT
            if (days.zipWithNext().any { (a, b) -> a >= b }) return DEFAULT
            return SyncWarningPolicy(days.toList())
        }

        /* Reads the stored form ("2,4,6"); unreadable or missing gives the default. */
        fun parse(stored: String?): SyncWarningPolicy =
            sanitize(stored?.split(",")?.map { it.trim().toIntOrNull() ?: return DEFAULT })
    }
}
