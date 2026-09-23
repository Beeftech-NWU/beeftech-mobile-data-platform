package com.beeftech.farmerregistration.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.repository.FarmerRepository
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.database.security.TokenProviderRegistry
import com.beeftech.farmerregistration.data.FarmerApiClient

class FarmerSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParams
) {

    override suspend fun doWork(): Result {

        return try {

            val database =
                DatabaseProvider.getDatabase()
                    ?: return Result.retry()

            val tokenProvider =
                TokenProviderRegistry.get()
                    ?: return Result.retry()

            val farmerRepository =
                FarmerRepository(
                    database.farmerDao()
                )

            val pendingSyncRepository =
                PendingSyncRepository(
                    database.pendingSyncDao()
                )

            val apiClient =
                FarmerApiClient(
                    context = applicationContext,
                    tokenProvider = tokenProvider
                )

            val pendingFarmers =
                farmerRepository.getPendingFarmers()

            if (pendingFarmers.isEmpty()) {

                /*
                 * Clean up any stale Farmer Registration
                 * pending-sync records whose farmer is
                 * already synchronized.
                 */
                val allPendingOperations =
                    pendingSyncRepository
                        .getAllPendingOperations()

                allPendingOperations
                    .filter {
                        it.entityType ==
                                FARMER_REGISTRATION_ENTITY_TYPE
                    }
                    .forEach { pendingOperation ->

                        pendingSyncRepository
                            .markSyncSuccessful(
                                pendingOperation.id
                            )
                    }

                return Result.success()
            }

            var hasFailure = false

            pendingFarmers.forEach { farmer ->

                val farmerId =
                    farmer.farmer_id

                try {

                    val addresses =
                        farmerRepository
                            .getAddressesForFarmer(
                                farmerId
                            )

                    val roles =
                        farmerRepository
                            .getRolesForFarmer(
                                farmerId
                            )

                    val syncResult =
                        apiClient.syncFarmer(
                            farmer = farmer,
                            addresses = addresses,
                            roles = roles
                        )

                    val syncSuccessful =
                        syncResult != null &&
                                syncResult.status.equals(
                                    "SYNCED",
                                    ignoreCase = true
                                )

                    if (syncSuccessful) {

                        /*
                         * Mark the actual farmer record
                         * as synchronized.
                         */
                        farmerRepository
                            .markAsSynced(
                                farmerId
                            )

                        /*
                         * Remove only the matching Farmer
                         * Registration entries from the
                         * shared pending-sync queue.
                         */
                        val pendingOperations =
                            pendingSyncRepository
                                .getAllPendingOperations()

                        pendingOperations
                            .filter {
                                it.entityType ==
                                        FARMER_REGISTRATION_ENTITY_TYPE &&
                                        it.entityId ==
                                        farmerId
                            }
                            .forEach { pendingOperation ->

                                pendingSyncRepository
                                    .markSyncSuccessful(
                                        pendingOperation.id
                                    )
                            }

                    } else {

                        farmerRepository
                            .markAsPending(
                                farmerId
                            )

                        /*
                         * Keep the Farmer Registration
                         * pending and increment its retry
                         * count.
                         */
                        markFarmerSyncFailed(
                            pendingSyncRepository =
                                pendingSyncRepository,
                            farmerId =
                                farmerId
                        )

                        hasFailure = true
                    }

                } catch (exception: Exception) {

                    farmerRepository
                        .markAsPending(
                            farmerId
                        )

                    markFarmerSyncFailed(
                        pendingSyncRepository =
                            pendingSyncRepository,
                        farmerId =
                            farmerId
                    )

                    hasFailure = true
                }
            }

            /*
             * If at least one Farmer Registration failed,
             * ask WorkManager to retry.
             *
             * Farmers that synchronized successfully are
             * already marked SYNCED and their matching
             * pending-sync entries have been removed.
             */
            if (hasFailure) {
                Result.retry()
            } else {
                Result.success()
            }

        } catch (exception: Exception) {

            Result.retry()
        }
    }

    private suspend fun markFarmerSyncFailed(
        pendingSyncRepository: PendingSyncRepository,
        farmerId: String
    ) {

        val pendingOperations =
            pendingSyncRepository
                .getAllPendingOperations()

        pendingOperations
            .filter {
                it.entityType ==
                        FARMER_REGISTRATION_ENTITY_TYPE &&
                        it.entityId ==
                        farmerId
            }
            .forEach { pendingOperation ->

                pendingSyncRepository
                    .markSyncFailed(
                        pendingOperation.id
                    )
            }
    }

    companion object {

        private const val FARMER_REGISTRATION_ENTITY_TYPE =
            "FARMER_REGISTRATION"
    }
}
