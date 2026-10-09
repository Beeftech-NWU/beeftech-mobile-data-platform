package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Adds gender, hide_colour, brand_mark, birth_weight_kg, age_class, body_condition, conformity, old_tag_number, reference_number, process_proof and implant_proof to calf_registrations
 * databases created before the sync carried them. Existing rows keep NULL.
 *
 * Idempotent, and a no-op when the table does not exist yet.
 */
object CalfRegistrationDetailsSchemaMigration {

    private val NEW_COLUMNS = listOf(
        "gender" to "VARCHAR(32) NULL",
        "hide_colour" to "VARCHAR(64) NULL",
        "brand_mark" to "VARCHAR(255) NULL",
        "birth_weight_kg" to "REAL NULL",
        "age_class" to "VARCHAR(64) NULL",
        "body_condition" to "VARCHAR(64) NULL",
        "conformity" to "VARCHAR(64) NULL",
        "old_tag_number" to "VARCHAR(255) NULL",
        "reference_number" to "VARCHAR(255) NULL",
        "process_proof" to "VARCHAR(512) NULL",
        "implant_proof" to "VARCHAR(512) NULL"
    )

    fun run(database: Database) = transaction(database) {
        val existing = exec("PRAGMA table_info(calf_registrations)") { rs ->
            buildList { while (rs.next()) add(rs.getString("name")) }
        } ?: emptyList()

        if (existing.isEmpty()) return@transaction

        NEW_COLUMNS
            .filter { (name, _) -> name !in existing }
            .forEach { (name, type) -> exec("ALTER TABLE calf_registrations ADD COLUMN $name $type") }
    }
}
