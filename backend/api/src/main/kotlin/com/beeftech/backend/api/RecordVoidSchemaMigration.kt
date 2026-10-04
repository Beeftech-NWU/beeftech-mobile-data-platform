package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Adds voided_at, voided_by_user_id and void_reason to the record tables created
 * before records could be voided. Existing rows stay un-voided (NULL).
 *
 * Idempotent, and a no-op for tables that do not exist yet.
 */
object RecordVoidSchemaMigration {

    private val TABLES = listOf(
        "calf_registrations",
        "animal_movements",
        "treatments",
        "farmers",
        "mortalities"
    )

    private val NEW_COLUMNS = listOf(
        "voided_at" to "BIGINT NULL",
        "voided_by_user_id" to "VARCHAR(64) NULL",
        "void_reason" to "TEXT NULL"
    )

    fun run(database: Database) = transaction(database) {
        TABLES.forEach { table ->
            val existing = exec("PRAGMA table_info($table)") { rs ->
                buildList { while (rs.next()) add(rs.getString("name")) }
            } ?: emptyList()

            if (existing.isEmpty()) return@forEach

            NEW_COLUMNS
                .filter { (name, _) -> name !in existing }
                .forEach { (name, type) -> exec("ALTER TABLE $table ADD COLUMN $name $type") }
        }
    }
}
