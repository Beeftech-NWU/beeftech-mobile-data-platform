package com.beeftech.backend.api.feedcrib

import com.beeftech.backend.api.RecordScope

class FeedCribService {

    /*
     * The submitter and site are kept beside the response, not in the
     * request or response DTOs, so a client can never set them.
     */
    private data class StoredReading(
        val reading: FeedCribResponse,
        val submittedByUserId: String?,
        val siteId: String?
    )

    private val records =
        mutableListOf<StoredReading>()

    private var nextId = 1L

    @Synchronized
    fun saveReading(
        request: FeedCribRequest,
        submittedByUserId: String? = null,
        siteId: String? = null
    ): FeedCribResponse {

        val record =
            FeedCribResponse(
                id = nextId++,
                penName = request.penName,
                adiValue = request.adiValue,
                morning = request.morning,
                midDay = request.midDay,
                evening = request.evening,
                timestamp = request.timestamp
            )

        records.add(
            StoredReading(
                reading = record,
                submittedByUserId = submittedByUserId,
                siteId = siteId
            )
        )

        return record
    }

    @Synchronized
    fun getAll(
        scope: RecordScope = RecordScope.All
    ): List<FeedCribResponse> {
        return records
            .filter { it.inScope(scope) }
            .map { it.reading }
    }

    @Synchronized
    fun getByPenName(
        penName: String,
        scope: RecordScope = RecordScope.All
    ): List<FeedCribResponse> {

        return records
            .filter { it.inScope(scope) }
            .map { it.reading }
            .filter {
                it.penName.equals(
                    penName,
                    ignoreCase = true
                )
            }
    }

    private fun StoredReading.inScope(scope: RecordScope): Boolean =
        when (scope) {
            RecordScope.All -> true
            is RecordScope.Site ->
                scope.siteId != null && siteId == scope.siteId
            is RecordScope.User ->
                submittedByUserId == scope.userId
        }
}
