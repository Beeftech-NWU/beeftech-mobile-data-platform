package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordVoidSchemaMigrationTest {

    private val tables = listOf("calf_registrations", "animal_movements", "treatments", "farmers", "mortalities")

    private fun legacyDatabase(): Database {
        val file = Files.createTempFile("beeftech-record-void-migration-test", ".db")
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
    fun `legacy tables gain the void columns and keep their rows un-voided`() {
        val database = legacyDatabase()

        RecordVoidSchemaMigration.run(database)

        tables.forEach { table ->
            assertTrue(
                columns(database, table).containsAll(listOf("voided_at", "voided_by_user_id", "void_reason")),
                "$table is missing a column"
            )
            val voidedAt = transaction(database) {
                exec("SELECT voided_at FROM $table WHERE record_id = 'r-1'") { rs ->
                    rs.next()
                    rs.getObject(1)
                }
            }
            assertEquals(null, voidedAt)
        }
    }

    @Test
    fun `running twice is a no-op`() {
        val database = legacyDatabase()

        RecordVoidSchemaMigration.run(database)
        RecordVoidSchemaMigration.run(database)

        tables.forEach { assertEquals(1, columns(database, it).count { c -> c == "voided_at" }) }
    }

    @Test
    fun `fresh database without the tables is left alone`() {
        val file = Files.createTempFile("beeftech-record-void-migration-fresh", ".db")
        file.toFile().deleteOnExit()
        val database = Database.connect("jdbc:sqlite:$file", driver = "org.sqlite.JDBC")

        RecordVoidSchemaMigration.run(database)

        tables.forEach { assertTrue(columns(database, it).isEmpty()) }
    }
}
