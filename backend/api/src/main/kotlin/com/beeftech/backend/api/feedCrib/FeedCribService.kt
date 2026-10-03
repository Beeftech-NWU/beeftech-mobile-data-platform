package com.beeftech.backend.api.feedcrib

import com.beeftech.backend.api.RecordScope

class FeedCribService(
    private val repository: FeedCribRepository
) {

    suspend fun saveReading(
        request: FeedCribRequest,
        submittedByUserId: String? = null,
        siteId: String? = null
    ): FeedCribResponse =
        repository.insert(request, submittedByUserId, siteId)

    suspend fun getAll(
        scope: RecordScope = RecordScope.All
    ): List<FeedCribResponse> =
        repository.list(scope)

    suspend fun getByPenName(
        penName: String,
        scope: RecordScope = RecordScope.All
    ): List<FeedCribResponse> =
        repository.list(scope, penName)
}
