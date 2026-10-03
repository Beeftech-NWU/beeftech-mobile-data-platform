package com.beeftech.backend.api.feedcrib

import com.beeftech.backend.api.DatabaseFactory
import com.beeftech.backend.api.RecordScope
import com.beeftech.backend.api.predicate
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/*
 * The submitter and site are stored beside the reading, not in the request or
 * response DTOs, so a client can never set them.
 */
class FeedCribRepository {

    suspend fun insert(
        request: FeedCribRequest,
        submittedByUserId: String?,
        siteId: String?
    ): FeedCribResponse =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            val id = FeedCribTable.insert {
                it[penName] = request.penName
                it[adiValue] = request.adiValue
                it[morning] = request.morning
                it[midDay] = request.midDay
                it[evening] = request.evening
                it[timestamp] = request.timestamp
                it[FeedCribTable.submittedByUserId] = submittedByUserId
                it[FeedCribTable.siteId] = siteId
                it[createdAt] = System.currentTimeMillis()
            } get FeedCribTable.id

            FeedCribResponse(
                id = id,
                penName = request.penName,
                adiValue = request.adiValue,
                morning = request.morning,
                midDay = request.midDay,
                evening = request.evening,
                timestamp = request.timestamp
            )
        }

    suspend fun list(scope: RecordScope, penName: String? = null): List<FeedCribResponse> =
        newSuspendedTransaction(Dispatchers.IO, db = DatabaseFactory.getDatabase()) {
            val scoped = scope.predicate(FeedCribTable.submittedByUserId, FeedCribTable.siteId)
            val filter: Op<Boolean> =
                if (penName == null) scoped
                else scoped and (FeedCribTable.penName.lowerCase() eq penName.lowercase())

            FeedCribTable
                .selectAll()
                .where { filter }
                .orderBy(FeedCribTable.id, SortOrder.ASC)
                .map { it.toResponse() }
        }

    private fun ResultRow.toResponse() = FeedCribResponse(
        id = this[FeedCribTable.id],
        penName = this[FeedCribTable.penName],
        adiValue = this[FeedCribTable.adiValue],
        morning = this[FeedCribTable.morning],
        midDay = this[FeedCribTable.midDay],
        evening = this[FeedCribTable.evening],
        timestamp = this[FeedCribTable.timestamp]
    )
}
