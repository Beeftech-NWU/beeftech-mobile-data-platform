package com.beeftech.backend.api.auth

import com.beeftech.backend.api.DatabaseFactory
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

object DevUserSeeder {

    private const val DEV_SITE_ID = "dev-site-1"
    private const val DEV_SITE_NAME = "Dev Feedlot"

    suspend fun seed(userRepository: UserRepository) {
        println("Seeding dev users...")

        seedDevSite()

        val devUsers = listOf(
            DevUser("admin", "10001", 1, siteId = null),
            DevUser("fmanager", "20002", 2, siteId = DEV_SITE_ID),
            DevUser("jvdm", "30003", 3, siteId = DEV_SITE_ID)
        )

        for (user in devUsers) {
            val existing = userRepository.findByUsername(user.username)
            if (existing == null) {
                userRepository.insertUser(
                    userId = UUID.randomUUID().toString(),
                    username = user.username,
                    pinHash = PinHasher.hash(user.pin),
                    role = user.role,
                    siteId = user.siteId
                )
                println("Seeded user: ${user.username}")
            } else {
                // Existing site assignments are managed by administrators.
                // Do not move GauFarm's manager back to the Feedcrib development site.
                println("Existing dev user retained: ${user.username}")
            }
        }
    }

    private suspend fun seedDevSite() {
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            val exists = SitesTable.selectAll()
                .where { SitesTable.siteId eq DEV_SITE_ID }
                .any()

            if (!exists) {
                SitesTable.insert {
                    it[siteId] = DEV_SITE_ID
                    it[name] = DEV_SITE_NAME
                    it[farmCode] = "S001"
                    it[createdAt] = System.currentTimeMillis()
                }
            }
        }
    }

    private data class DevUser(
        val username: String,
        val pin: String,
        val role: Int,
        val siteId: String?
    )
}
