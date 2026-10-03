package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordScopeSchemaMigrationTest {

    private val tables = listOf("calf_registrations", "animal_movements", "treatments", "farmers")

    private fun legacyDatabase(): Database {
        val file = Files.createTempFile("beeftech-record-scope-migration-test", ".db")
        file.toFile().deleteOnExit()
        val database = Database.connect("jdbc:sqlite:$file", driver = "org.sqlite.JDBC")

        transaction(database) {
            tables.forEach { table ->
                exec("CREATE TABLE $table (record_id VARCHAR(64) NOT NULL PRIMARY KEY)")
                exec("INSERT INTO $table (record_id) VALUES ('r-1')")
            }
        }
        return database
    }

    private fun columns(database: Database, table: String): List<String> =
        transaction(database) {
            exec("PRAGMA table_info($table)") { rs ->
                buildList { while (rs.next()) add(rs.getString("name")) }
            } ?: emptyList()
        }

    @Test
    fun `legacy tables gain both columns and keep their rows with null scope`() {
        val database = legacyDatabase()

        RecordScopeSchemaMigration.run(database)

        tables.forEach { table ->
            assertTrue(
                columns(database, table).containsAll(listOf("submitted_by_user_id", "site_id")),
                "$table is missing a column"
            )
            val row = transaction(database) {
                exec("SELECT submitted_by_user_id, site_id FROM $table WHERE record_id = 'r-1'") { rs ->
                    rs.next()
                    rs.getString(1) to rs.getString(2)
                }
            }
            assertEquals(null to null, row)
        }
    }

    @Test
    fun `running twice is a no-op`() {
        val database = legacyDatabase()

        RecordScopeSchemaMigration.run(database)
        RecordScopeSchemaMigration.run(database)

        tables.forEach { table ->
            assertEquals(1, columns(database, table).count { it == "site_id" })
        }
    }

    @Test
    fun `fresh database without the tables is left alone`() {
        val file = Files.createTempFile("beeftech-record-scope-migration-fresh", ".db")
        file.toFile().deleteOnExit()
        val database = Database.connect("jdbc:sqlite:$file", driver = "org.sqlite.JDBC")

        RecordScopeSchemaMigration.run(database)

        tables.forEach { assertTrue(columns(database, it).isEmpty()) }
    }
}
