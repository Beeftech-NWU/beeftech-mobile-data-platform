package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

/** Adds the optional medicine field to existing backend SQLite databases without deleting records. */
object TreatmentMedicineSchemaMigration {
    fun run(database: Database) = transaction(database) {
        val columns = exec("PRAGMA table_info(treatments)") { rs ->
            buildSet { while (rs.next()) add(rs.getString("name")) }
        } ?: emptySet()
        if (columns.isNotEmpty() && "medicine_used" !in columns) {
            exec("ALTER TABLE treatments ADD COLUMN medicine_used VARCHAR(255) NOT NULL DEFAULT ''")
        }
    }
}
