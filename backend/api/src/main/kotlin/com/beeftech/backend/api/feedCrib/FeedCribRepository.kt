package com.beeftech.backend.api.feedcrib

import com.beeftech.backend.api.DatabaseFactory
import com.beeftech.backend.api.RecordOwnershipException
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class FeedCribRepository {

    suspend fun listCribs(siteId: String): List<FeedCribDto> =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            FeedCribsTable
                .selectAll()
                .where { FeedCribsTable.siteId eq siteId }
                .orderBy(FeedCribsTable.cribNumber, SortOrder.ASC)
                .map { it.toCribDto() }
        }

    suspend fun listCodes(): List<CribReadingCodeDto> =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            CribReadingCodesTable
                .selectAll()
                .orderBy(CribReadingCodesTable.code, SortOrder.ASC)
                .map {
                    CribReadingCodeDto(
                        code = it[CribReadingCodesTable.code],
                        label = it[CribReadingCodesTable.label],
                        description = it[CribReadingCodesTable.description],
                        active = it[CribReadingCodesTable.active]
                    )
                }
        }

    /*
     * Every phone on the site, not just the caller: the app's 9-reading history is meant to show
     * readings other workers took. Voided entries are left out.
     */
    suspend fun listEntries(siteId: String, fromReadingDate: String): List<FeedCribEntryDto> =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            FeedCribEntriesTable
                .selectAll()
                .where {
                    (FeedCribEntriesTable.siteId eq siteId) and
                        (FeedCribEntriesTable.readingDate greaterEq fromReadingDate) and
                        FeedCribEntriesTable.voidedAt.isNull()
                }
                .orderBy(FeedCribEntriesTable.capturedAt, SortOrder.ASC)
                .map { it.toEntryDto() }
        }

    /*
     * Validates and upserts one entry by record GUID, so a retried sync never creates a duplicate.
     * Throws FeedCribValidationException for a record the server rejects. Validation, the upsert
     * and the crib's current_adi move together in one transaction.
     */
    suspend fun upsertEntry(
        record: FeedCribEntrySyncRecord,
        deviceId: String,
        submittedBy: String?,
        submitterSiteId: String?,
        serverSyncedAt: Long
    ): Long =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {

            val siteId = submitterSiteId
                ?: throw FeedCribValidationException("Your account has no site, so feed crib readings cannot be saved.")

            val crib = FeedCribsTable
                .selectAll()
                .where { (FeedCribsTable.siteId eq siteId) and (FeedCribsTable.cribNumber eq record.cribNumber) }
                .singleOrNull()
                ?: throw FeedCribValidationException("Crib ${record.cribNumber} does not exist on this site.")

            if (!crib[FeedCribsTable.active]) {
                throw FeedCribValidationException("Crib ${record.cribNumber} is not active.")
            }

            if (record.code != null) {
                val codeRow = CribReadingCodesTable
                    .selectAll()
                    .where { CribReadingCodesTable.code eq record.code }
                    .singleOrNull()
                if (codeRow == null || !codeRow[CribReadingCodesTable.active]) {
                    throw FeedCribValidationException("Reading code ${record.code} is not valid.")
                }
            }

            val existing = FeedCribEntriesTable
                .selectAll()
                .where { FeedCribEntriesTable.recordguid eq record.recordguid }
                .singleOrNull()

            if (existing == null) {
                FeedCribEntriesTable.insert {
                    it[recordguid] = record.recordguid
                    it[cribNumber] = record.cribNumber
                    it[FeedCribEntriesTable.siteId] = siteId
                    it[readingDate] = record.readingDate
                    it[slot] = record.slot
                    it[code] = record.code
                    it[adi] = record.adi
                    it[capturedAt] = record.capturedAt
                    it[FeedCribEntriesTable.deviceId] = deviceId
                    it[gpsLat] = record.gpsLat
                    it[gpsLng] = record.gpsLng
                    it[submittedByUserId] = submittedBy
                    it[syncStatus] = "SYNCED"
                    it[syncedAt] = serverSyncedAt
                }
            } else {
                if (existing[FeedCribEntriesTable.siteId] != siteId ||
                    existing[FeedCribEntriesTable.submittedByUserId] != submittedBy ||
                    existing[FeedCribEntriesTable.voidedAt] != null
                ) {
                    throw RecordOwnershipException(
                        "Record ${record.recordguid} belongs to another user or site, or was voided, and cannot be overwritten."
                    )
                }

                FeedCribEntriesTable.update({ FeedCribEntriesTable.recordguid eq record.recordguid }) {
                    it[cribNumber] = record.cribNumber
                    it[readingDate] = record.readingDate
                    it[slot] = record.slot
                    it[code] = record.code
                    it[adi] = record.adi
                    it[capturedAt] = record.capturedAt
                    it[FeedCribEntriesTable.deviceId] = deviceId
                    it[gpsLat] = record.gpsLat
                    it[gpsLng] = record.gpsLng
                    it[syncStatus] = "SYNCED"
                    it[syncedAt] = serverSyncedAt
                }
            }

            refreshCurrentAdi(siteId, record.cribNumber, serverSyncedAt)

            serverSyncedAt
        }

    /* current_adi follows whichever live entry for the crib was captured last, so a late-arriving older entry cannot roll it back. */
    private fun refreshCurrentAdi(siteId: String, cribNumber: String, now: Long) {
        val newest = FeedCribEntriesTable
            .selectAll()
            .where {
                (FeedCribEntriesTable.siteId eq siteId) and
                    (FeedCribEntriesTable.cribNumber eq cribNumber) and
                    FeedCribEntriesTable.voidedAt.isNull()
            }
            .orderBy(FeedCribEntriesTable.capturedAt, SortOrder.DESC)
            .orderBy(FeedCribEntriesTable.id, SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?: return

        FeedCribsTable.update({ (FeedCribsTable.siteId eq siteId) and (FeedCribsTable.cribNumber eq cribNumber) }) {
            it[currentAdi] = newest[FeedCribEntriesTable.adi]
            it[updatedAt] = now
        }
    }

    private fun ResultRow.toCribDto() = FeedCribDto(
        cribNumber = this[FeedCribsTable.cribNumber],
        siteId = this[FeedCribsTable.siteId],
        penDescription = this[FeedCribsTable.penDescription],
        ration = this[FeedCribsTable.ration],
        method = this[FeedCribsTable.method],
        description = this[FeedCribsTable.description],
        requiredKg = this[FeedCribsTable.requiredKg],
        animalsBegin = this[FeedCribsTable.animalsBegin],
        animalsIn = this[FeedCribsTable.animalsIn],
        animalsOut = this[FeedCribsTable.animalsOut],
        animalsClose = this[FeedCribsTable.animalsClose],
        currentAdi = this[FeedCribsTable.currentAdi],
        active = this[FeedCribsTable.active],
        updatedAt = this[FeedCribsTable.updatedAt]
    )

    private fun ResultRow.toEntryDto() = FeedCribEntryDto(
        recordguid = this[FeedCribEntriesTable.recordguid],
        cribNumber = this[FeedCribEntriesTable.cribNumber],
        readingDate = this[FeedCribEntriesTable.readingDate],
        slot = this[FeedCribEntriesTable.slot],
        code = this[FeedCribEntriesTable.code],
        adi = this[FeedCribEntriesTable.adi],
        capturedAt = this[FeedCribEntriesTable.capturedAt],
        deviceId = this[FeedCribEntriesTable.deviceId],
        submittedByUserId = this[FeedCribEntriesTable.submittedByUserId],
        syncedAt = this[FeedCribEntriesTable.syncedAt]
    )
}

/** A record the server rejects; the sync reports it as ERROR for that record only. */
class FeedCribValidationException(message: String) : RuntimeException(message)
