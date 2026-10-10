package com.beeftech.backend.api.feedcrib

import java.time.DateTimeException
import java.time.LocalDate
import java.time.ZoneOffset

class FeedCribService(
    private val repository: FeedCribRepository
) {

    /* One bad record reports ERROR for itself and does not stop the rest of the batch. */
    suspend fun sync(
        request: FeedCribEntrySyncRequest,
        submittedBy: String?,
        siteId: String?
    ): FeedCribEntrySyncResponse {

        val results = request.records.map { record ->

            try {

                validate(record)

                val deviceId = record.deviceId.ifBlank { request.deviceId }

                val syncedAt = repository.upsertEntry(
                    record,
                    deviceId,
                    submittedBy,
                    siteId,
                    System.currentTimeMillis()
                )

                FeedCribEntrySyncResult(
                    recordguid = record.recordguid,
                    cribNumber = record.cribNumber,
                    status = "SYNCED",
                    serverSyncedAt = syncedAt
                )

            } catch (exception: Exception) {

                FeedCribEntrySyncResult(
                    recordguid = record.recordguid,
                    cribNumber = record.cribNumber,
                    status = "ERROR",
                    message = exception.message ?: "Unable to sync feed crib entry."
                )
            }
        }

        return FeedCribEntrySyncResponse(results = results)
    }

    /**
     * The site's cribs and the code table, plus every entry from the last [days] days on the site.
     * The date cut-off is a day wider than asked, because device dates are local and the server's
     * are UTC; the app picks its own last 3 days out of what it gets.
     */
    suspend fun load(siteId: String, days: Int = DEFAULT_DAYS): FeedCribsResponse {
        val now = System.currentTimeMillis()
        val from = LocalDate.now(ZoneOffset.UTC).minusDays(days.coerceIn(1, MAX_DAYS).toLong())

        return FeedCribsResponse(
            siteId = siteId,
            cribs = repository.listCribs(siteId),
            codes = repository.listCodes(),
            entries = repository.listEntries(siteId, from.toString()),
            serverTime = now
        )
    }

    private fun validate(record: FeedCribEntrySyncRecord) {
        if (record.recordguid.isBlank()) throw FeedCribValidationException("Missing recordguid.")
        if (record.cribNumber.isBlank()) throw FeedCribValidationException("Missing crib number.")
        if (FeedSlot.entries.none { it.name == record.slot }) {
            throw FeedCribValidationException("Slot must be MORNING, MIDDAY or EVENING, not '${record.slot}'.")
        }
        try {
            LocalDate.parse(record.readingDate)
        } catch (_: DateTimeException) {
            throw FeedCribValidationException("Reading date must be yyyy-MM-dd, not '${record.readingDate}'.")
        }
        if (!record.adi.isFinite() || record.adi < 0) {
            throw FeedCribValidationException("ADI must be zero or more.")
        }
        if (record.capturedAt <= 0) throw FeedCribValidationException("Missing capture time.")
    }

    companion object {
        const val DEFAULT_DAYS = 3
        const val MAX_DAYS = 14
    }
}
