package com.beeftech.farmtraceability.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.database.security.TokenProviderRegistry
import com.beeftech.farmtraceability.data.TreatmentApiClient
import com.beeftech.farmtraceability.data.TreatmentRepository

class TreatmentSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParams
) {

    override suspend fun doWork(): Result {

        /*
         * The BeefTech database is encrypted with SQLCipher.
         *
         * The worker only uses the database after it has already
         * been securely unlocked and registered with DatabaseProvider.
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
                TreatmentRepository(
                    treatmentDao =
                        database.treatmentDao(),

                    pendingSyncRepository =
                        pendingSyncRepository,

                    apiClient =
                        TreatmentApiClient(
                            tokenProvider = tokenProvider
                        )
                )

            /*
             * Upload all locally stored PENDING Treatment records.
             *
             * Successful records are marked SYNCED by the repository.
             * Failed records remain stored locally for another attempt.
             */
            repository.syncPending()

            val remainingTreatmentOperations =
                pendingSyncRepository
                    .getPendingOperations()
                    .any { pendingOperation ->

                        pendingOperation.entityType ==
                                ENTITY_TYPE
                    }

            /*
             * If Treatment operations remain pending,
             * ask WorkManager to retry later.
             */
            if (remainingTreatmentOperations) {

                Result.retry()

            } else {

                Result.success()
            }

        } catch (_: Exception) {

            /*
             * Never delete the local Treatment because synchronization
             * failed. WorkManager can retry it later.
             */
            Result.retry()
        }
    }

    companion object {

        private const val ENTITY_TYPE =
            "TREATMENT"
    }
}
