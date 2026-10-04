package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Adds audit_log.details (a JSON object of what changed) to databases created before
 * admin actions were audited. Existing rows keep NULL.
 *
 * Idempotent, and a no-op when the table does not exist yet.
 */
object AuditLogSchemaMigration {

    fun run(database: Database) = transaction(database) {
        val existing = exec("PRAGMA table_info(audit_log)") { rs ->
            buildList { while (rs.next()) add(rs.getString("name")) }
        } ?: emptyList()

        if (existing.isNotEmpty() && "details" !in existing) {
            exec("ALTER TABLE audit_log ADD COLUMN details TEXT NULL")
        }
    }
}
