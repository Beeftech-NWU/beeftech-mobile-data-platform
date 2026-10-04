package com.beeftech.farmtraceability.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.database.security.TokenProviderRegistry
import com.beeftech.farmtraceability.data.MortalityApiClient
import com.beeftech.farmtraceability.data.MortalityRepository

class MortalitySyncWorker(
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

        return try {

            val pendingSyncRepository =
                PendingSyncRepository(
                    database.pendingSyncDao()
                )

            MortalityRepository(
                mortalityDao = database.mortalityDao(),
                pendingSyncRepository = pendingSyncRepository,
                apiClient = MortalityApiClient(tokenProvider = tokenProvider)
            ).syncPending()

            val remaining =
                pendingSyncRepository
                    .getPendingOperations()
                    .any { it.entityType == MortalityRepository.ENTITY_TYPE }

            if (remaining) Result.retry() else Result.success()

        } catch (_: Exception) {

            /* Never delete the local record because sync failed; WorkManager retries later. */
            Result.retry()
        }
    }
}
