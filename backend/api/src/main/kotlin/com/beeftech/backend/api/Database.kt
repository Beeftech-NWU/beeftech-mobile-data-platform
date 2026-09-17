package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseFactory {

    fun init(jdbcUrl: String = "jdbc:sqlite:./data/beeftech-backend.db") {

        val filePath = jdbcUrl.removePrefix("jdbc:sqlite:")

        // Skip directory creation for in-memory databases (e.g. ":memory:",
        // or SQLite's shared-cache "file::memory:?cache=shared" form) - there
        // is no file on disk to create a parent directory for.
        if (!filePath.contains(":memory:")) {

            File(filePath).parentFile?.mkdirs()
        }

        Database.connect(
            url = jdbcUrl,
            driver = "org.sqlite.JDBC"
        )

        transaction {
            SchemaUtils.create(CalfRegistrationTable)
        }
    }
}
