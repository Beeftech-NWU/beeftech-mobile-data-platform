package com.beeftech.management.data

import com.beeftech.database.dao.SyncSecurityDao
import com.beeftech.database.entity.SyncSecurityEvent

sealed interface EventUploadOutcome {
    /* Nothing was waiting for this user. */
    data object NothingToSend : EventUploadOutcome

    data class Uploaded(val count: Int) : EventUploadOutcome

    /* No connection, no token, or the server refused: the events stay waiting for the next try. */
    data class Failed(val reason: String) : EventUploadOutcome
}

/**
 * Sends the signed-in user's security events (warnings, the Day-7 wipe and lock) to the server.
 *
 * It only flags an event as uploaded once the server has said it holds it, and never edits or
 * deletes one. A failure leaves everything as it was, so the next check-in just tries again.
 */
class SecurityEventSync(
    private val apiClient: ManagementApiClient,
    private val dao: SyncSecurityDao,
    private val userId: String,
    private val now: () -> Long = System::currentTimeMillis
) {

    suspend fun upload(): EventUploadOutcome {
        var sent = 0

        while (true) {
            val batch = dao.getNotUploaded(userId, BATCH_SIZE)
            if (batch.isEmpty()) break

            when (val result = apiClient.uploadSecurityEvents(batch.map { it.toUpload() })) {
                is ManagementResult.Success -> {
                    val answered = result.value.accepted + result.value.duplicates + result.value.rejected
                    /* Anything short of a full answer isn't flagged, so it is sent again. */
                    if (answered < batch.size) return EventUploadOutcome.Failed("The server did not take every event")

                    dao.markUploaded(batch.map { it.id }, now())
                    sent += batch.size
                }
                is ManagementResult.NoConnection -> return EventUploadOutcome.Failed("No connection")
                is ManagementResult.Unauthorized -> return EventUploadOutcome.Failed("Signed out")
                is ManagementResult.Forbidden -> return EventUploadOutcome.Failed(result.message)
                is ManagementResult.NotFound -> return EventUploadOutcome.Failed("The server can't take security events yet")
                is ManagementResult.Rejected -> return EventUploadOutcome.Failed(result.message)
                is ManagementResult.Error -> return EventUploadOutcome.Failed(result.message)
            }
        }

        return if (sent == 0) EventUploadOutcome.NothingToSend else EventUploadOutcome.Uploaded(sent)
    }

    private fun SyncSecurityEvent.toUpload() = SecurityEventUpload(
        eventKey = eventKey,
        eventType = eventType,
        eventTime = eventTime,
        warningDay = warningDay,
        pendingCount = pendingCount,
        oldestPendingCreatedAt = oldestPendingCreatedAt,
        details = details,
        userId = userId
    )

    companion object {
        /* The server takes at most 200 per request. */
        const val BATCH_SIZE = 200
    }
}
