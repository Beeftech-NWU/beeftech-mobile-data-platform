package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Adds submitted_by_user_id and site_id to the synced record tables created
 * before records were stamped with their submitter and site. Existing rows
 * keep NULL for both; they are not backfilled.
 *
 * Idempotent, and a no-op for tables that do not exist yet.
 */
object RecordScopeSchemaMigration {

    private val TABLES = listOf(
        "calf_registrations",
        "animal_movements",
        "treatments",
        "farmers"
    )

    private val NEW_COLUMNS = listOf("submitted_by_user_id", "site_id")

    fun run(database: Database) = transaction(database) {
        TABLES.forEach { table ->
            val existing = exec("PRAGMA table_info($table)") { rs ->
                buildList { while (rs.next()) add(rs.getString("name")) }
            } ?: emptyList()

            if (existing.isEmpty()) return@forEach

            NEW_COLUMNS
                .filter { it !in existing }
                .forEach { exec("ALTER TABLE $table ADD COLUMN $it VARCHAR(64) NULL") }
        }
    }
}
