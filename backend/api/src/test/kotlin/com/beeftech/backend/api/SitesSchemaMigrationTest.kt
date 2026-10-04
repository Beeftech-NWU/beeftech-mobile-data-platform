package com.beeftech.backend.api

import com.beeftech.backend.api.auth.SitesSchemaMigration
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SitesSchemaMigrationTest {

    private fun database(): Database {
        val file = Files.createTempFile("beeftech-sites-migration-test", ".db")
        file.toFile().deleteOnExit()
        return Database.connect("jdbc:sqlite:$file", driver = "org.sqlite.JDBC")
    }

    private fun legacyDatabase(): Database {
        val database = database()
        transaction(database) {
            exec("CREATE TABLE sites (site_id VARCHAR(64) NOT NULL PRIMARY KEY, name VARCHAR(255) NOT NULL, created_at BIGINT NOT NULL)")
            exec("INSERT INTO sites (site_id, name, created_at) VALUES ('dev-site-1', 'Dev Feedlot', 5)")
        }
        return database
    }

    private fun columns(database: Database): List<String> =
        transaction(database) {
            exec("PRAGMA table_info(sites)") { rs ->
                buildList { while (rs.next()) add(rs.getString("name")) }
            } ?: emptyList()
        }

    @Test
    fun `a legacy sites table gains active and updated_at and existing sites stay active`() {
        val database = legacyDatabase()

        SitesSchemaMigration.run(database)

        assertTrue(columns(database).containsAll(listOf("active", "updated_at")))
        val row = transaction(database) {
            exec("SELECT name, active, updated_at FROM sites WHERE site_id = 'dev-site-1'") { rs ->
                rs.next()
                Triple(rs.getString("name"), rs.getInt("active"), rs.getObject("updated_at"))
            }
        }
        assertEquals(Triple("Dev Feedlot", 1, null), row)
    }

    @Test
    fun `running twice is a no-op`() {
        val database = legacyDatabase()

        SitesSchemaMigration.run(database)
        SitesSchemaMigration.run(database)

        assertEquals(1, columns(database).count { it == "active" })
        assertEquals(1, columns(database).count { it == "updated_at" })
    }

    @Test
    fun `a fresh database without the table is left alone`() {
        val database = database()

        SitesSchemaMigration.run(database)

        assertTrue(columns(database).isEmpty())
    }
}
