package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Adds the farmer registration columns (co-reg/ID, land ownership, FA code,
 * GLN, herd capacity, interest status, contact name and number, farm size, head count, primary breed,
 * street code, postal address, country) to farmers / farmer_addresses
 * databases created before they existed. SchemaUtils.create never alters an
 * existing table, so without this the inserts fail on an already-deployed DB.
 *
 * Only missing columns are added, so it is idempotent. A no-op on a fresh
 * database where the tables do not exist yet.
 */
object FarmerSchemaMigration {

    private const val TEXT = "VARCHAR(255) NULL"

    private val NEW_COLUMNS = mapOf(
        "farmers" to listOf(
            "co_reg_id_no" to TEXT,
            "land_ownership" to TEXT,
            "fa_code_rmis" to TEXT,
            "gln_number" to TEXT,
            "herd_capacity" to "INTEGER NULL",
            "interest_status" to "VARCHAR(64) NULL",
            "contact_name" to TEXT,
            "contact_number" to "VARCHAR(32) NULL",
            "farm_size_ha" to "DOUBLE PRECISION NULL",
            "head_count" to "INTEGER NULL",
            "primary_breed" to TEXT
        ),
        "farmer_addresses" to listOf(
            "street_code" to TEXT,
            "postal_address" to TEXT,
            "country" to TEXT
        )
    )

    fun run(database: Database) = transaction(database) {
        NEW_COLUMNS.forEach { (table, newColumns) ->
            val existing = exec("PRAGMA table_info($table)") { rs ->
                buildList { while (rs.next()) add(rs.getString("name")) }
            } ?: emptyList()

            if (existing.isEmpty()) return@forEach

            newColumns
                .filter { (name, _) -> name !in existing }
                .forEach { (name, type) -> exec("ALTER TABLE $table ADD COLUMN $name $type") }
        }
    }
}
