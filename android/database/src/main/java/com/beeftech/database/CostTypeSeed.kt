package com.beeftech.database

import androidx.sqlite.db.SupportSQLiteDatabase

/*
 * Seed rows for cost_types (remediation plan, Phase 5).
 *
 * Used by MIGRATION_13_14 (upgrades) and by
 * BeefTechDatabase.SEED_CALLBACK (fresh installs).
 * INSERT OR IGNORE keeps it safe to run more than once.
 */
object CostTypeSeed {

    /* code to display name, in display order. */
    val TYPES: List<Pair<String, String>> = listOf(
        "TRANSPORT" to "Transport",
        "PROCESSING" to "Processing",
        "HANDLING" to "Handling",
        "INTEREST" to "Interest",
        "TREATMENT" to "Treatment",
        "FEED" to "Feed / Ration",
        "DIRECT" to "Direct",
        "INDIRECT" to "Indirect",
        "FUEL_MAINTENANCE" to "Fuel & maintenance"
    )

    fun execute(db: SupportSQLiteDatabase) {
        TYPES.forEachIndexed { index, (code, name) ->
            db.execSQL(
                "INSERT OR IGNORE INTO `cost_types` (`code`, `display_name`, `sort_order`, `is_active`) VALUES (?, ?, ?, 1)",
                arrayOf<Any>(code, name, index)
            )
        }
    }
}
