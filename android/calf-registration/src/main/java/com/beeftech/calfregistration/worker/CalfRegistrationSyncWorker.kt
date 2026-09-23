package com.beeftech.calfregistration.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.beeftech.calfregistration.data.CalfRegistrationApiClient
import com.beeftech.calfregistration.data.CalfRegistrationRepository
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.database.security.TokenProviderRegistry

class CalfRegistrationSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        /*
         * The BeefTech database is encrypted with SQLCipher.
         *
         * DatabaseProvider contains the database only after the user has
         * successfully unlocked/initialised it through the application's
         * secure database flow.
         *
         * The Worker must never attempt to store, retrieve, or bypass the
         * user's database passcode.
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

            val repository =
                CalfRegistrationRepository(
                    calfRegistrationDao =
                        database.calfRegistrationDao(),
                    pendingSyncRepository =
                        pendingSyncRepository,
                    apiClient =
                        CalfRegistrationApiClient(
                            tokenProvider = tokenProvider
                        )
                )

            /*
             * Reuse the existing calf-registration synchronization logic.
             *
             * syncPending():
             * - finds unsynced calf registrations
             * - sends them to the backend
             * - marks successful records as SYNCED
             * - updates PendingSync bookkeeping
             */
            repository.syncPending()

            val remainingCalfOperations =
                pendingSyncRepository
                    .getPendingOperations()
                    .any { pendingOperation ->
                        pendingOperation.entityType ==
                                ENTITY_TYPE
                    }

            /*
             * Remaining records normally mean the network/server sync
             * did not complete successfully.
             *
             * WorkManager can retry later using its configured backoff.
             */
            if (remainingCalfOperations) {
                Result.retry()
            } else {
                Result.success()
            }

        } catch (_: Exception) {

            /*
             * Keep the pending records intact.
             * WorkManager can retry the operation later.
             */
            Result.retry()
        }
    }

    companion object {

        private const val ENTITY_TYPE =
            "CALF_REGISTRATION"
    }
}
