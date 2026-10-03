package com.beeftech.backend.api

import com.beeftech.backend.api.auth.UsersSchemaMigration
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UsersSchemaMigrationTest {

    private fun newDatabase(): Database {
        val file = Files.createTempFile("beeftech-users-migration-test", ".db")
        file.toFile().deleteOnExit()
        return Database.connect("jdbc:sqlite:$file", driver = "org.sqlite.JDBC")
    }

    private fun columns(database: Database): List<String> =
        transaction(database) {
            exec("PRAGMA table_info(users)") { rs ->
                buildList { while (rs.next()) add(rs.getString("name")) }
            } ?: emptyList()
        }

    @Test
    fun `legacy users table gains site_id and keeps its rows`() {
        val database = newDatabase()
        transaction(database) {
            exec(
                """
                CREATE TABLE users (
                    user_id VARCHAR(64) NOT NULL PRIMARY KEY,
                    username VARCHAR(255) NOT NULL,
                    pin_hash VARCHAR(72) NOT NULL,
                    role INT NULL
                )
                """.trimIndent()
            )
            exec("INSERT INTO users (user_id, username, pin_hash, role) VALUES ('u-1', 'jvdm', 'x', 3)")
        }

        UsersSchemaMigration.run(database)

        assertTrue("site_id" in columns(database))
        val row = transaction(database) {
            exec("SELECT username, site_id FROM users WHERE user_id = 'u-1'") { rs ->
                rs.next()
                rs.getString(1) to rs.getString(2)
            }
        }
        assertEquals("jvdm" to null, row)
    }

    @Test
    fun `running twice is a no-op`() {
        val database = newDatabase()
        transaction(database) {
            exec("CREATE TABLE users (user_id VARCHAR(64) NOT NULL PRIMARY KEY)")
        }

        UsersSchemaMigration.run(database)
        UsersSchemaMigration.run(database)

        assertEquals(1, columns(database).count { it == "site_id" })
    }

    @Test
    fun `fresh database without the table is left alone`() {
        val database = newDatabase()

        UsersSchemaMigration.run(database)

        assertTrue(columns(database).isEmpty())
    }
}
