package com.beeftech.backend.api.auth

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Adds sites.active, sites.updated_at, sites.farm_code and sites.sales_rep_email to databases created before they existed.
 * Existing sites stay active. Sites without a farm code get S001, S002, ... in the order they were created,
 * and the unique index on farm_code is created once every row has one.
 *
 * Idempotent, and a no-op on a fresh database where sites does not exist yet.
 */
object SitesSchemaMigration {

    fun run(database: Database) = transaction(database) {
        val existing = exec("PRAGMA table_info(sites)") { rs ->
            buildList { while (rs.next()) add(rs.getString("name")) }
        } ?: emptyList()

        if (existing.isEmpty()) return@transaction

        if ("active" !in existing) {
            exec("ALTER TABLE sites ADD COLUMN active BOOLEAN NOT NULL DEFAULT 1")
        }

        if ("updated_at" !in existing) {
            exec("ALTER TABLE sites ADD COLUMN updated_at BIGINT NULL")
        }

        if ("farm_code" !in existing) {
            exec("ALTER TABLE sites ADD COLUMN farm_code VARCHAR(4) NULL")
        }

        if ("sales_rep_email" !in existing) {
            exec("ALTER TABLE sites ADD COLUMN sales_rep_email VARCHAR(255) NULL")
        }

        backfillFarmCodes()
        exec("CREATE UNIQUE INDEX IF NOT EXISTS sites_farm_code_unique ON sites (farm_code)")
    }

    /* Never reuses a code a site already has, so it is safe to run again after sites were added. */
    private fun org.jetbrains.exposed.sql.Transaction.backfillFarmCodes() {
        val taken = exec("SELECT farm_code FROM sites WHERE farm_code IS NOT NULL") { rs ->
            buildSet { while (rs.next()) add(rs.getString(1)) }
        }?.toMutableSet() ?: mutableSetOf()

        val missing = exec("SELECT site_id FROM sites WHERE farm_code IS NULL ORDER BY created_at, site_id") { rs ->
            buildList { while (rs.next()) add(rs.getString(1)) }
        } ?: emptyList()

        var sequence = 1
        missing.forEach { siteId ->
            var code: String
            do {
                code = "S%03d".format(sequence++)
            } while (code in taken)
            taken += code
            exec("UPDATE sites SET farm_code = '$code' WHERE site_id = '${siteId.replace("'", "''")}'")
        }
    }
}
