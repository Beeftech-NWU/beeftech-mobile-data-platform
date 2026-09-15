package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseFactory {

    fun init(jdbcUrl: String = "jdbc:sqlite:./data/beeftech-backend.db") {

        val filePath = jdbcUrl.removePrefix("jdbc:sqlite:")

        if (filePath != ":memory:" && !filePath.contains(":memory:")) {

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
