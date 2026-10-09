package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FarmerSchemaMigrationTest {

    private fun legacyDatabase(): Database {
        val file = Files.createTempFile("beeftech-farmer-migration-test", ".db")
        file.toFile().deleteOnExit()
        val database = Database.connect("jdbc:sqlite:$file", driver = "org.sqlite.JDBC")

        transaction(database) {
            exec(
                """
                CREATE TABLE farmers (
                    farmer_id VARCHAR(255) NOT NULL PRIMARY KEY,
                    client_code VARCHAR(255) NULL,
                    organisation_name VARCHAR(255) NULL,
                    vat_number VARCHAR(255) NULL,
                    email_address VARCHAR(255) NULL,
                    gps_latitude REAL NULL,
                    gps_longitude REAL NULL,
                    sync_status VARCHAR(50) DEFAULT 'SYNCED' NOT NULL,
                    synced_at BIGINT NULL
                )
                """.trimIndent()
            )
            exec(
                """
                CREATE TABLE farmer_addresses (
                    address_id VARCHAR(255) NOT NULL PRIMARY KEY,
                    farmer_id VARCHAR(255) NOT NULL,
                    address_type VARCHAR(255) NULL,
                    address_line_1 VARCHAR(500) NULL,
                    province VARCHAR(255) NULL,
                    postal_code VARCHAR(50) NULL,
                    gps_latitude REAL NULL,
                    gps_longitude REAL NULL
                )
                """.trimIndent()
            )
            exec("INSERT INTO farmers (farmer_id, client_code) VALUES ('f-1', 'KAR001')")
            exec(
                "INSERT INTO farmer_addresses (address_id, farmer_id, address_line_1) " +
                    "VALUES ('a-1', 'f-1', 'Mark Str 93')"
            )
        }
        return database
    }

    private fun columns(database: Database, table: String): List<String> =
        transaction(database) {
            exec("PRAGMA table_info($table)") { rs ->
                buildList { while (rs.next()) add(rs.getString("name")) }
            } ?: emptyList()
        }

    private val expectedFarmerColumns =
        listOf("co_reg_id_no", "land_ownership", "fa_code_rmis", "gln_number", "herd_capacity", "interest_status",
            "contact_name", "contact_number", "farm_size_ha", "head_count", "primary_breed",
            "sales_notified_at")

    private val expectedAddressColumns =
        listOf("street_code", "postal_address", "country")

    @Test
    fun `legacy tables gain the new columns and keep their rows`() {
        val database = legacyDatabase()

        FarmerSchemaMigration.run(database)

        assertTrue(columns(database, "farmers").containsAll(expectedFarmerColumns))
        assertTrue(columns(database, "farmer_addresses").containsAll(expectedAddressColumns))

        val farmer = transaction(database) {
            exec("SELECT client_code, co_reg_id_no FROM farmers WHERE farmer_id = 'f-1'") { rs ->
                rs.next()
                rs.getString(1) to rs.getString(2)
            }
        }
        assertEquals("KAR001" to null, farmer)

        val address = transaction(database) {
            exec("SELECT address_line_1, country FROM farmer_addresses WHERE address_id = 'a-1'") { rs ->
                rs.next()
                rs.getString(1) to rs.getString(2)
            }
        }
        assertEquals("Mark Str 93" to null, address)
    }

    @Test
    fun `running the migration twice is a no-op`() {
        val database = legacyDatabase()

        FarmerSchemaMigration.run(database)
        FarmerSchemaMigration.run(database)

        assertEquals(
            1,
            columns(database, "farmers").count { it == "co_reg_id_no" }
        )
    }

    @Test
    fun `fresh database without the tables is left alone`() {
        val file = Files.createTempFile("beeftech-farmer-migration-fresh", ".db")
        file.toFile().deleteOnExit()
        val database = Database.connect("jdbc:sqlite:$file", driver = "org.sqlite.JDBC")

        FarmerSchemaMigration.run(database)

        assertTrue(columns(database, "farmers").isEmpty())
    }
}
