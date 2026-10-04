package com.beeftech.database

import androidx.sqlite.db.SupportSQLiteDatabase

object RationSeed {

    private val defaults =
        listOf(
            "RATION-STARTER" to "Starter Ration",
            "RATION-GROWER" to "Grower Mix",
            "RATION-FINISHER" to "Finisher Ration",
            "RATION-MAINTENANCE" to "Maintenance Ration",
            "RATION-BACKGROUNDING" to "Backgrounding Ration"
        )


    fun execute(
        db: SupportSQLiteDatabase
    ) {

        defaults.forEach { ration ->

            db.execSQL(
                """
                INSERT OR IGNORE INTO rations
                (
                    ration_id,
                    name,
                    active
                )
                VALUES (?, ?, 1)
                """.trimIndent(),

                arrayOf<Any?>(
                    ration.first,
                    ration.second
                )
            )
        }
    }
}
