package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Adds the farmer registration columns (co-reg/ID, land ownership, FA code,
 * GLN, street code, postal address, country) to farmers / farmer_addresses
 * databases created before they existed. SchemaUtils.create never alters an
 * existing table, so without this the inserts fail on an already-deployed DB.
 *
 * Only missing columns are added, so it is idempotent. A no-op on a fresh
 * database where the tables do not exist yet.
 */
object FarmerSchemaMigration {

    private val NEW_COLUMNS = mapOf(
        "farmers" to listOf(
            "co_reg_id_no",
            "land_ownership",
            "fa_code_rmis",
            "gln_number"
        ),
        "farmer_addresses" to listOf(
            "street_code",
            "postal_address",
            "country"
        )
    )

    fun run(database: Database) = transaction(database) {
        NEW_COLUMNS.forEach { (table, newColumns) ->
            val existing = exec("PRAGMA table_info($table)") { rs ->
                buildList { while (rs.next()) add(rs.getString("name")) }
            } ?: emptyList()

            if (existing.isEmpty()) return@forEach

            newColumns
                .filter { it !in existing }
                .forEach { exec("ALTER TABLE $table ADD COLUMN $it VARCHAR(255) NULL") }
        }
    }
}
