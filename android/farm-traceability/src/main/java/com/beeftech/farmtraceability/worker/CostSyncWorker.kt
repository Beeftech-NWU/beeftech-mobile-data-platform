package com.beeftech.farmtraceability.worker

import com.beeftech.database.entity.SyncRunModule
import com.beeftech.database.entity.SyncRunTrigger
import com.beeftech.database.repository.SyncRunRepository
import com.beeftech.database.repository.SyncRunSummary
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.database.security.TokenProviderRegistry
import com.beeftech.farmtraceability.data.CostApiClient
import com.beeftech.farmtraceability.data.CostRepository

class CostSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParams
) {

    override suspend fun doWork(): Result {

        /*
         * The BeefTech database is encrypted with SQLCipher. The worker only
         * uses it after it has been unlocked and registered with
         * DatabaseProvider.
         */
        val database =
            DatabaseProvider.getDatabase()
                ?: return Result.failure()

        val tokenProvider =
            TokenProviderRegistry.get()
                ?: return Result.retry()

        /*
         * Offline login grants local access only, so there may be no valid
         * server token. Leave everything queued and untouched until an
         * online sign-in provides one.
         */
        if (tokenProvider.token() == null) {
            SyncRunRepository(database.syncRunDao(), database.pendingSyncDao()).recordOffline(
                module = SyncRunModule.COST,
                trigger = inputData.getString(SyncRunSummary.TRIGGER_INPUT_KEY) ?: SyncRunTrigger.AUTO,
                firstAttempt = runAttemptCount == 0
            )
            return Result.retry()
        }

        return try {

            val pendingSyncRepository =
                PendingSyncRepository(
                    database.pendingSyncDao()
                )

            val syncRuns =
                SyncRunRepository(
                    database.syncRunDao(),
                    database.pendingSyncDao()
                )

            val trigger =
                inputData.getString(SyncRunSummary.TRIGGER_INPUT_KEY)
                    ?: SyncRunTrigger.AUTO

            syncRuns.trackRun(SyncRunModule.COST, listOf(CostRepository.ENTITY_TYPE), trigger) {
                CostRepository(
                    animalCostDao = database.animalCostDao(),
                    pendingSyncRepository = pendingSyncRepository,
                    apiClient = CostApiClient(tokenProvider = tokenProvider)
                ).syncPending()
            }

            val remaining =
                pendingSyncRepository
                    .getPendingOperations()
                    .any { it.entityType == CostRepository.ENTITY_TYPE }

            if (remaining) Result.retry() else Result.success()

        } catch (_: Exception) {

            /* Never delete the local record because sync failed; WorkManager retries later. */
            Result.retry()
        }
    }
}
