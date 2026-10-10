package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalfRegistrationDetailsSchemaMigrationTest {

    private val newColumns = listOf("gender", "hide_colour", "brand_mark", "birth_weight_kg", "age_class", "body_condition", "conformity", "old_tag_number", "reference_number", "process_proof", "implant_proof")

    private fun database(createTable: Boolean): Database {
        val file = Files.createTempFile("beeftech-calf-details-migration-test", ".db")
        file.toFile().deleteOnExit()
        val database = Database.connect("jdbc:sqlite:$file", driver = "org.sqlite.JDBC")
        if (createTable) {
            transaction(database) {
                exec("CREATE TABLE calf_registrations (id INTEGER PRIMARY KEY AUTOINCREMENT, tag_number VARCHAR(255) NOT NULL)")
                exec("INSERT INTO calf_registrations (tag_number) VALUES ('Blu0000064')")
            }
        }
        return database
    }

    private fun columns(database: Database): List<String> =
        transaction(database) {
            exec("PRAGMA table_info(calf_registrations)") { rs ->
                buildList { while (rs.next()) add(rs.getString("name")) }
            } ?: emptyList()
        }

    @Test
    fun `existing table gains the detail columns and keeps its rows with NULL values`() {
        val database = database(createTable = true)

        CalfRegistrationDetailsSchemaMigration.run(database)

        assertTrue(columns(database).containsAll(newColumns))
        val row = transaction(database) {
            exec("SELECT tag_number, gender, birth_weight_kg FROM calf_registrations") { rs ->
                rs.next()
                Triple(rs.getString(1), rs.getString(2), rs.getObject(3))
            }
        }
        assertEquals(Triple("Blu0000064", null, null), row)
    }

    @Test
    fun `running twice is a no-op`() {
        val database = database(createTable = true)

        CalfRegistrationDetailsSchemaMigration.run(database)
        CalfRegistrationDetailsSchemaMigration.run(database)

        newColumns.forEach { assertEquals(1, columns(database).count { c -> c == it }) }
    }

    @Test
    fun `fresh database without the table is left alone`() {
        val database = database(createTable = false)

        CalfRegistrationDetailsSchemaMigration.run(database)

        assertTrue(columns(database).isEmpty())
    }
}
