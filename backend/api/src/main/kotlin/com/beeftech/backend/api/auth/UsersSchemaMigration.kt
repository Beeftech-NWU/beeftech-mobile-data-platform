package com.beeftech.backend.api.auth

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Adds users.site_id and users.active to databases created before they existed.
 * SchemaUtils.create never alters an existing table, so without this the
 * user queries fail on an already-deployed DB. Existing users stay active.
 *
 * Idempotent, and a no-op on a fresh database where users does not exist yet.
 */
object UsersSchemaMigration {

    fun run(database: Database) = transaction(database) {
        val existing = exec("PRAGMA table_info(users)") { rs ->
            buildList { while (rs.next()) add(rs.getString("name")) }
        } ?: emptyList()

        if (existing.isEmpty()) return@transaction

        if ("site_id" !in existing) {
            exec("ALTER TABLE users ADD COLUMN site_id VARCHAR(64) NULL")
        }

        if ("active" !in existing) {
            exec("ALTER TABLE users ADD COLUMN active BOOLEAN NOT NULL DEFAULT 1")
        }
    }
}
