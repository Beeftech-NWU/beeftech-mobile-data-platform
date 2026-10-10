package com.beeftech.feedcrib.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.entity.SyncRunModule
import com.beeftech.database.entity.SyncRunTrigger
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.database.repository.SyncRunRepository
import com.beeftech.database.repository.SyncRunSummary
import com.beeftech.database.security.TokenProviderRegistry
import com.beeftech.feedcrib.data.FeedCribApiClient
import com.beeftech.feedcrib.data.FeedCribCaptureContext
import com.beeftech.feedcrib.data.FeedCribRepository

class FeedCribSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParams
) {

    override suspend fun doWork(): Result {

        /*
         * The BeefTech database is encrypted with SQLCipher. The worker only uses it after it
         * has been unlocked and registered with DatabaseProvider.
         */
        val database = DatabaseProvider.getDatabase()

        if (database == null) {
            Log.w(TAG, "Database is not initialized. Feed crib sync will retry.")
            return Result.retry()
        }

        val tokenProvider = TokenProviderRegistry.get()

        if (tokenProvider == null) {
            Log.w(TAG, "Authentication token provider is unavailable. Feed crib sync will retry.")
            return Result.retry()
        }

        return try {

            val pendingSyncRepository = PendingSyncRepository(database.pendingSyncDao())

            val syncRuns = SyncRunRepository(database.syncRunDao(), database.pendingSyncDao())

            val trigger = inputData.getString(SyncRunSummary.TRIGGER_INPUT_KEY) ?: SyncRunTrigger.AUTO

            val repository = FeedCribRepository(
                feedCribDao = database.feedCribDao(),
                pendingSyncRepository = pendingSyncRepository,
                apiClient = FeedCribApiClient(tokenProvider = tokenProvider),
                /* The worker only sends existing readings. It never creates one from this context. */
                captureContextProvider = { FeedCribCaptureContext(deviceId = "") }
            )

            syncRuns.trackRun(SyncRunModule.FEED, listOf(FeedCribRepository.ENTITY_TYPE), trigger) {
                repository.syncPending()
            }

            val remaining = pendingSyncRepository
                .getPendingOperations()
                .any { it.entityType == FeedCribRepository.ENTITY_TYPE }

            if (remaining) Result.retry() else Result.success()

        } catch (exception: Exception) {

            /* Never delete a local reading because sync failed; WorkManager retries later. */
            Log.e(TAG, "Feed crib sync worker failed. Readings stay queued and will retry.", exception)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "FeedCribSyncWorker"
    }
}
