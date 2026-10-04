package com.beeftech.backend.api.auth

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Adds users.site_id, users.active and users.tokens_valid_after and users.sync_lock_cleared_at to databases created before they existed.
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

        if ("tokens_valid_after" !in existing) {
            exec("ALTER TABLE users ADD COLUMN tokens_valid_after BIGINT NULL")
        }

        if ("sync_lock_cleared_at" !in existing) {
            exec("ALTER TABLE users ADD COLUMN sync_lock_cleared_at BIGINT NULL")
        }
    }
}
