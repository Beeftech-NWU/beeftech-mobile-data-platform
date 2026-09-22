package com.beeftech.farmtraceability.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.farmtraceability.data.AnimalMovementApiClient
import com.beeftech.farmtraceability.data.AnimalMovementRepository

class AnimalMovementSyncWorker(
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

        return try {

            val pendingSyncRepository =
                PendingSyncRepository(
                    database.pendingSyncDao()
                )

            val repository =
                AnimalMovementRepository(
                    animalMovementDao =
                        database.animalMovementDao(),

                    pendingSyncRepository =
                        pendingSyncRepository,

                    apiClient =
                        AnimalMovementApiClient()
                )

            /*
             * Upload all locally stored PENDING movement records.
             *
             * Successful records are marked SYNCED by the repository.
             * Failed records remain available for another attempt.
             */
            repository.syncPending()

            val remainingMovementOperations =
                pendingSyncRepository
                    .getPendingOperations()
                    .any { pendingOperation ->

                        pendingOperation.entityType ==
                                ENTITY_TYPE
                    }

            /*
             * If movement operations remain pending,
             * ask WorkManager to retry later.
             */
            if (remainingMovementOperations) {

                Result.retry()

            } else {

                Result.success()
            }

        } catch (_: Exception) {

            /*
             * Never delete the local movement because synchronization
             * failed. WorkManager can retry it later.
             */
            Result.retry()
        }
    }

    companion object {

        private const val ENTITY_TYPE =
            "ANIMAL_MOVEMENT"
    }
}