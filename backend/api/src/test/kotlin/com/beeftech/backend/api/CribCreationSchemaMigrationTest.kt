package com.beeftech.backend.api

import com.beeftech.backend.api.feedcrib.CribCreationSchemaMigration
import org.jetbrains.exposed.sql.Database
import java.nio.file.Files
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CribCreationSchemaMigrationTest {
    @Test fun legacyCribAndReadingsReceiveSameStableUuidWithoutDataLoss() {
        val file = Files.createTempFile("crib-creation-migration", ".db").toFile().also { it.deleteOnExit() }
        val url = "jdbc:sqlite:${file.absolutePath}"
        DriverManager.getConnection(url).use { connection ->
            connection.createStatement().use {
                it.execute("CREATE TABLE feed_cribs (id INTEGER PRIMARY KEY, site_id TEXT, crib_number TEXT, current_adi DOUBLE)")
                it.execute("INSERT INTO feed_cribs VALUES (1, 'site-1', 'A009', 1.5)")
                it.execute("CREATE TABLE feed_crib_entries (recordguid TEXT PRIMARY KEY, site_id TEXT, crib_number TEXT)")
                it.execute("INSERT INTO feed_crib_entries VALUES ('reading-1', 'site-1', 'A009')")
            }
        }
        val db = Database.connect(url, driver = "org.sqlite.JDBC")
        CribCreationSchemaMigration.run(db)
        CribCreationSchemaMigration.run(db)
        DriverManager.getConnection(url).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT c.crib_uuid, e.crib_uuid, c.current_adi FROM feed_cribs c JOIN feed_crib_entries e ON c.site_id = e.site_id").use {
                    assertTrue(it.next())
                    assertEquals(UUID.nameUUIDFromBytes("site-1|A009".toByteArray()).toString(), it.getString(1))
                    assertEquals(it.getString(1), it.getString(2))
                    assertEquals(1.5, it.getDouble(3))
                }
            }
        }
    }
}

