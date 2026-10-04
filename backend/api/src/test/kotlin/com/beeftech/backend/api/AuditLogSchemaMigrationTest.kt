package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuditLogSchemaMigrationTest {

    private fun database(): Database {
        val file = Files.createTempFile("beeftech-audit-migration-test", ".db")
        file.toFile().deleteOnExit()
        return Database.connect("jdbc:sqlite:$file", driver = "org.sqlite.JDBC")
    }

    private fun legacyDatabase(): Database {
        val database = database()
        transaction(database) {
            exec(
                "CREATE TABLE audit_log (id INTEGER PRIMARY KEY AUTOINCREMENT, action VARCHAR(50) NOT NULL, " +
                    "entity_type VARCHAR(50) NOT NULL, entity_id VARCHAR(255) NOT NULL, reason TEXT NOT NULL, " +
                    "actor_user_id VARCHAR(64) NOT NULL, actor_username VARCHAR(255) NOT NULL, " +
                    "actor_role INT NOT NULL, site_id VARCHAR(64) NULL, created_at BIGINT NOT NULL)"
            )
            exec(
                "INSERT INTO audit_log (action, entity_type, entity_id, reason, actor_user_id, actor_username, " +
                    "actor_role, created_at) VALUES ('VOID', 'MORTALITY', 'g-1', 'Wrong animal', 'u1', 'fmanager', 2, 5)"
            )
        }
        return database
    }

    private fun columns(database: Database): List<String> =
        transaction(database) {
            exec("PRAGMA table_info(audit_log)") { rs ->
                buildList { while (rs.next()) add(rs.getString("name")) }
            } ?: emptyList()
        }

    @Test
    fun `a legacy table gains details and keeps its rows`() {
        val database = legacyDatabase()

        AuditLogSchemaMigration.run(database)

        assertTrue("details" in columns(database))
        val row = transaction(database) {
            exec("SELECT reason, details FROM audit_log WHERE entity_id = 'g-1'") { rs ->
                rs.next()
                rs.getString("reason") to rs.getString("details")
            }
        }
        assertEquals("Wrong animal" to null, row)
    }

    @Test
    fun `running twice is a no-op`() {
        val database = legacyDatabase()

        AuditLogSchemaMigration.run(database)
        AuditLogSchemaMigration.run(database)

        assertEquals(1, columns(database).count { it == "details" })
    }

    @Test
    fun `a fresh database without the table is left alone`() {
        val database = database()

        AuditLogSchemaMigration.run(database)

        assertTrue(columns(database).isEmpty())
    }
}
