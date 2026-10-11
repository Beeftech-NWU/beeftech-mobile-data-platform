package com.beeftech.backend.api.feedcrib

import com.beeftech.backend.api.DatabaseFactory
import com.beeftech.backend.api.auth.SitesTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID
import java.util.Locale

class CribRegistrationRepository {
    /** Deployment configuration until approved rules are supplied; no ADI formula is inferred. */
    private fun adiRules(): CribAdiRules {
        val minimum = System.getenv("BEEFTECH_ADI_MIN")?.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 } ?: 0.0
        return CribAdiRules(
            units = System.getenv("BEEFTECH_ADI_UNITS")?.takeIf { it.isNotBlank() },
            minimum = minimum,
            maximum = System.getenv("BEEFTECH_ADI_MAX")?.toDoubleOrNull()?.takeIf { it.isFinite() && it >= minimum },
            adjustmentStep = System.getenv("BEEFTECH_ADI_STEP")?.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 } ?: 0.1
        )
    }
    suspend fun setup(): CribSetup = newSuspendedTransaction(Dispatchers.IO, DatabaseFactory.getDatabase()) {
        CribSetup(
            sites = SitesTable.selectAll().where { SitesTable.active eq true }
                .map { CribSite(it[SitesTable.siteId], it[SitesTable.name]) },
            pens = CribPensTable.selectAll().map { CribPen(it[CribPensTable.id], it[CribPensTable.siteId],
                it[CribPensTable.description], it[CribPensTable.animalsBegin], it[CribPensTable.animalsIn],
                it[CribPensTable.animalsOut], it[CribPensTable.animalsClose], it[CribPensTable.requiredKg], it[CribPensTable.active]) },
            rations = CribRationsTable.selectAll().map { CribRation(it[CribRationsTable.id], it[CribRationsTable.name], it[CribRationsTable.active]) },
            methods = FeedCribsTable.selectAll().map { it[FeedCribsTable.method] }.filter { it.isNotBlank() }.distinct(),
            adiRules = adiRules()
        )
    }

    suspend fun create(record: CribRegistration, userId: String): CribRegistrationResult {
        return try {
            require(UUID.fromString(record.cribUuid).toString() == record.cribUuid) { "Invalid crib UUID." }
            val code = record.cribNumber.trim().uppercase(Locale.ROOT)
            val tag = record.rfid?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.isNotEmpty() }
            require(code.isNotEmpty() && code.length <= 32) { "Crib code must contain 1–32 characters." }
            require(record.initialAdi.isFinite() && record.initialAdi >= 0) { "Initial ADI must be zero or greater (temporary rule)." }
            val rules = adiRules()
            require(record.initialAdi >= rules.minimum && (rules.maximum == null || record.initialAdi <= rules.maximum)) { "Initial ADI is outside the configured range." }
            require(record.description.length <= 512 && record.method.length <= 255) { "Description or method is too long." }
            require(tag == null || tag.length <= 255) { "RFID tag is too long." }
            require(record.createdAt > 0 && record.deviceId.isNotBlank() && record.deviceId.length <= 255) { "Creation time and device ID are required." }
            require((record.gpsLat == null) == (record.gpsLng == null)) { "Location must contain both coordinates or be unavailable." }
            require(record.gpsLat == null || (record.gpsLat.isFinite() && record.gpsLat in -90.0..90.0 &&
                record.gpsLng!!.isFinite() && record.gpsLng in -180.0..180.0)) { "Invalid GPS coordinates." }
            newSuspendedTransaction(Dispatchers.IO, DatabaseFactory.getDatabase()) {
                val previous = FeedCribsTable.selectAll().where { FeedCribsTable.cribUuid eq record.cribUuid }.singleOrNull()
                if (previous != null) {
                    require(previous[FeedCribsTable.createdBy] == userId &&
                        previous[FeedCribsTable.siteId] == record.siteId && previous[FeedCribsTable.cribNumber] == code &&
                        previous[FeedCribsTable.penId] == record.penId && previous[FeedCribsTable.rationId] == record.rationId &&
                        previous[FeedCribsTable.initialAdi] == record.initialAdi && previous[FeedCribsTable.rfid] == tag &&
                        previous[FeedCribsTable.description] == record.description && previous[FeedCribsTable.method] == record.method &&
                        previous[FeedCribsTable.createdAt] == record.createdAt && previous[FeedCribsTable.creationDeviceId] == record.deviceId &&
                        previous[FeedCribsTable.creationGpsLat] == record.gpsLat && previous[FeedCribsTable.creationGpsLng] == record.gpsLng
                    ) { "UUID already exists with different creation data; resolution required." }
                    return@newSuspendedTransaction CribRegistrationResult(record.cribUuid, "SYNCED")
                }
                require(SitesTable.selectAll().where { (SitesTable.siteId eq record.siteId) and (SitesTable.active eq true) }.any()) { "Select an existing active site." }
                require(CribPensTable.selectAll().where { (CribPensTable.id eq record.penId) and
                    (CribPensTable.siteId eq record.siteId) and (CribPensTable.active eq true) }.any()) { "Pen does not belong to this site or is inactive." }
                require(CribRationsTable.selectAll().where { (CribRationsTable.id eq record.rationId) and
                    (CribRationsTable.active eq true) }.any()) { "Select an existing active ration." }
                require(record.method.isEmpty() || FeedCribsTable.selectAll().where { FeedCribsTable.method eq record.method }.any()) {
                    "Select an existing feeding method or leave it unspecified."
                }
                if (FeedCribsTable.selectAll().where { (FeedCribsTable.siteId eq record.siteId) and
                    (FeedCribsTable.cribNumber.trim().upperCase() eq code) }.any()) {
                    return@newSuspendedTransaction CribRegistrationResult(record.cribUuid, "CONFLICT", "Crib code already exists on this site. Resolve the conflict before uploading readings.")
                }
                if (tag != null && FeedCribsTable.selectAll().where { FeedCribsTable.rfid eq tag }.any()) {
                    return@newSuspendedTransaction CribRegistrationResult(record.cribUuid, "CONFLICT", "RFID tag already identifies another crib. Resolution required.")
                }
                FeedCribsTable.insert {
                    it[cribUuid] = record.cribUuid; it[cribNumber] = code; it[siteId] = record.siteId
                    it[penId] = record.penId; it[rationId] = record.rationId
                    it[initialAdi] = record.initialAdi; it[currentAdi] = record.initialAdi
                    it[description] = record.description; it[method] = record.method; it[rfid] = tag
                    it[createdBy] = userId; it[createdAt] = record.createdAt
                    it[creationDeviceId] = record.deviceId; it[creationGpsLat] = record.gpsLat; it[creationGpsLng] = record.gpsLng
                    it[active] = true; it[updatedAt] = System.currentTimeMillis()
                }
                CribRegistrationResult(record.cribUuid, "SYNCED")
            }
        } catch (e: Exception) {
            // Includes database uniqueness races: never acknowledge or merge a different UUID.
            val uniqueViolation = generateSequence<Throwable>(e) { it.cause }.any {
                it is java.sql.SQLException && (it.errorCode == 19 || it.message.orEmpty().contains("UNIQUE constraint"))
            }
            // REJECTED: a validation failure (every require above) that retrying the same record cannot fix.
            // ERROR: anything else, treated as temporary.
            val status = when {
                uniqueViolation -> "CONFLICT"
                e is IllegalArgumentException -> "REJECTED"
                else -> "ERROR"
            }
            CribRegistrationResult(record.cribUuid, status, e.message ?: "Unable to register crib.")
        }
    }
}

