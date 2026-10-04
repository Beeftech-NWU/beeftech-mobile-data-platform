package com.beeftech.backend.api.auth

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Adds sites.active and sites.updated_at to databases created before sites could be managed.
 * Existing sites stay active.
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
    }
}
