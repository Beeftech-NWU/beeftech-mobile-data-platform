package com.example.app.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.app.data.local.PendingSyncDao
import com.example.app.data.remote.FarmerApiClient
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltWorker
class FarmerSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pendingSyncDao: PendingSyncDao,
    private val farmerApiClient: FarmerApiClient
) : CoroutineWorker(context, params) {

    companion object {
        const val MAX_RETRY_COUNT = 5
        private const val SYNC_ENTITY_TYPE = "FARMER"
        private const val STATUS_PENDING = "PENDING"
        private const val STATUS_PROCESSING = "PROCESSING"
        private const val STATUS_FAILED = "FAILED"
    }

    override async suspend doWork(): Result = withContext(Dispatchers.IO) {
        Timber.d("Starting FarmerSyncWorker execution...")

        // 1. Fetch pending sync items isolated to the farmer entity type
        val pendingSyncItems = pendingSyncDao.getPendingSyncItemsByType(
            entityType = SYNC_ENTITY_TYPE,
            status = STATUS_PENDING
        )

        if (pendingSyncItems.isEmpty()) {
            Timber.d("No pending farmer sync records found. Exiting worker with success.")
            return@withContext Result.success()
        }

        var totalItemsProcessed = 0
        var totalFailures = 0
        var exceedMaxRetries = false

        for (syncItem in pendingSyncItems) {
            // Check max retries cap before attempting dispatch
            if (syncItem.retryCount >= MAX_RETRY_COUNT) {
                Timber.w("Sync record ID %s reached max retries (%d). Marking permanently failed.", syncItem.id, MAX_RETRY_COUNT)
                pendingSyncDao.updateSyncStatus(
                    id = syncItem.id,
                    status = STATUS_FAILED,
                    errorMessage = "Exceeded maximum retry attempts (${MAX_RETRY_COUNT})"
                )
                exceedMaxRetries = true
                continue
            }

            // 2. Mark item as PROCESSING before making the API request
            pendingSyncDao.updateSyncStatus(
                id = syncItem.id,
                status = STATUS_PROCESSING,
                errorMessage = null
            )

            // 3. Dispatch network sync request
            val success = try {
                val farmerPayload = pendingSyncDao.getFarmerPayloadById(syncItem.entityId)
                if (farmerPayload != null) {
                    val response = farmerApiClient.syncFarmer(farmerPayload)
                    response.isSuccessful
                } else {
                    Timber.e("Payload missing for entity ID: %s", syncItem.entityId)
                    false
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception caught during sync call for entity ID: %s", syncItem.entityId)
                false
            }

            // 4. Update persistence boundary based on sync response
            if (success) {
                Timber.d("Successfully synced farmer entity ID: %s", syncItem.entityId)
                pendingSyncDao.deletePendingSyncItem(syncItem.id)
                totalItemsProcessed++
            } else {
                totalFailures++
                val updatedRetryCount = syncItem.retryCount + 1
                
                if (updatedRetryCount >= MAX_RETRY_COUNT) {
                    Timber.e("Permanent sync failure for entity ID: %s after %d attempts.", syncItem.entityId, updatedRetryCount)
                    pendingSyncDao.updateSyncStatusAndRetryCount(
                        id = syncItem.id,
                        status = STATUS_FAILED,
                        retryCount = updatedRetryCount,
                        errorMessage = "Network request failed on final attempt."
                    )
                    exceedMaxRetries = true
                } else {
                    Timber.w("Sync attempt %d failed for entity ID: %s. Reverting status to PENDING.", updatedRetryCount, syncItem.entityId)
                    pendingSyncDao.updateSyncStatusAndRetryCount(
                        id = syncItem.id,
                        status = STATUS_PENDING,
                        retryCount = updatedRetryCount,
                        errorMessage = "Transient network error during sync."
                    )
                }
            }
        }

        // 5. Worker Result decision
        return@withContext when {
            totalFailures > 0 && !exceedMaxRetries -> {
                Timber.w("Worker completed with transient failures. Scheduling retry with exponential backoff.")
                Result.retry()
            }
            exceedMaxRetries && totalItemsProcessed == 0 -> {
                Timber.e("Worker finished with unrecoverable failures.")
                Result.failure()
            }
            else -> {
                Timber.d("Worker execution completed successfully for %d items.", totalItemsProcessed)
                Result.success()
            }
        }
    }
}