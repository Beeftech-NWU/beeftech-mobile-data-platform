package com.beeftech.backend.api

import com.beeftech.backend.api.auth.SitesTable
import com.beeftech.backend.api.auth.UsersSchemaMigration
import com.beeftech.backend.api.feedcrib.FeedCribTable
import com.beeftech.backend.api.auth.UsersTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseFactory {

    @Volatile
    private var db: Database? = null

    fun init(
        jdbcUrl: String =
            "jdbc:sqlite:./data/beeftech-backend.db"
    ): Database {

        val filePath =
            jdbcUrl.removePrefix("jdbc:sqlite:")

        if (!filePath.contains(":memory:")) {
            File(filePath)
                .parentFile
                ?.mkdirs()
        }

        val database = Database.connect(
            url = jdbcUrl,
            driver = "org.sqlite.JDBC"
        )
        db = database

        // Must run before SchemaUtils.create, which never alters an existing table.
        CalfRegistrationSchemaMigration.run(database)
        FarmerSchemaMigration.run(database)
        UsersSchemaMigration.run(database)
        RecordScopeSchemaMigration.run(database)

        transaction(database) {

            SchemaUtils.create(
                CalfRegistrationTable,
                AnimalMovementTable,
                TreatmentTable,
                DiseaseTable,
                TreatmentTypeTable,
                FarmerTable,
                FarmerAddressTable,
                FarmerRoleTable,
                UsersTable,
                SitesTable,
                FeedCribTable,
                MortalityTable,
                CostTable
            )
        }

        /*
         * Seed treatment reference/master data
         * after the reference tables exist.
         */
        TreatmentReferenceSeeder.seed()

        return database
    }

    fun getDatabase(): Database? = db
}
