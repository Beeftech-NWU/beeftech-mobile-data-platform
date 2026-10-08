package com.beeftech.backend.api

import com.beeftech.backend.api.auth.DevicesTable
import com.beeftech.backend.api.auth.LoginAttemptsTable
import com.beeftech.backend.api.auth.LoginEventsTable
import com.beeftech.backend.api.auth.SitesSchemaMigration
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
        CalfRegistrationDetailsSchemaMigration.run(database)
        FarmerSchemaMigration.run(database)
        UsersSchemaMigration.run(database)
        SitesSchemaMigration.run(database)
        RecordScopeSchemaMigration.run(database)
        RecordVoidSchemaMigration.run(database)
        AuditLogSchemaMigration.run(database)

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
                CostTable,
                AuditLogTable,
                DevicesTable,
                LoginAttemptsTable,
                LoginEventsTable,
                CostTypeTable,
                AppSettingsTable,
                SyncSecurityEventsTable,
                SyncUploadLogTable
            )
        }

        /*
         * Seed treatment reference/master data
         * after the reference tables exist.
         */
        ReferenceDataSeeder.seed()

        return database
    }

    fun getDatabase(): Database? = db
}
