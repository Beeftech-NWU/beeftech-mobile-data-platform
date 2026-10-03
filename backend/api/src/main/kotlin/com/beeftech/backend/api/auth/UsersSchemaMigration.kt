package com.beeftech.backend.api.auth

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Adds users.site_id to databases created before sites existed.
 * SchemaUtils.create never alters an existing table, so without this the
 * user queries fail on an already-deployed DB.
 *
 * Idempotent, and a no-op on a fresh database where users does not exist yet.
 */
object UsersSchemaMigration {

    fun run(database: Database) = transaction(database) {
        val existing = exec("PRAGMA table_info(users)") { rs ->
            buildList { while (rs.next()) add(rs.getString("name")) }
        } ?: emptyList()

        if (existing.isEmpty() || "site_id" in existing) return@transaction

        exec("ALTER TABLE users ADD COLUMN site_id VARCHAR(64) NULL")
    }
}
