package com.beeftech.farmerregistration.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.entity.PendingSync
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

            /*
             * pending_sync is the ownership boundary.
             *
             * FarmerEntity itself does not contain user ownership.
             * Therefore never begin synchronization by selecting every
             * PENDING Farmer stored on the physical device.
             */
            val farmerOperations =
                pendingSyncRepository
                    .getAllPendingOperations()
                    .filter {
                        it.entityType ==
                                FARMER_REGISTRATION_ENTITY_TYPE
                    }

            if (farmerOperations.isEmpty()) {
                return Result.success()
            }

            /*
             * Resolve only Farmers referenced by the active user's
             * pending-sync queue.
             */
            val queuedFarmers =
                farmerOperations
                    .map {
                            pendingOperation ->

                        pendingOperation to
                                farmerRepository
                                    .getFarmerById(
                                        pendingOperation.entityId
                                    )
                    }

            /*
             * A queue entry is stale if the Farmer is already SYNCED
             * or the corresponding local Farmer no longer exists.
             *
             * Only entries from this user-scoped queue snapshot may
             * be removed.
             */
            queuedFarmers
                .filter {
                        (_, farmer) ->

                    farmer == null ||
                            farmer.sync_status ==
                            "SYNCED"
                }
                .forEach {
                        (pendingOperation, _) ->

                    pendingSyncRepository
                        .markSyncSuccessful(
                            pendingOperation.id
                        )
                }

            val pendingFarmers =
                queuedFarmers
                    .mapNotNull {
                            (_, farmer) ->

                        farmer
                            ?.takeIf {
                                it.sync_status ==
                                        "PENDING" ||
                                        it.sync_status ==
                                        "PROCESSING"
                            }
                    }
                    .distinctBy {
                        it.farmer_id
                    }

            if (pendingFarmers.isEmpty()) {
                return Result.success()
            }

            var hasFailure = false

            pendingFarmers.forEach { farmer ->

                val farmerId =
                    farmer.farmer_id

                try {

                    farmerRepository
                        .markAsProcessing(
                            farmerId
                        )

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
                        /*
                         * Remove only queue entries captured for the
                         * account that started this synchronization.
                         */
                        farmerOperations
                            .filter {
                                it.entityId ==
                                        farmerId
                            }
                            .forEach {
                                    pendingOperation ->

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
                            pendingOperations =
                                farmerOperations,
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
                        pendingOperations =
                            farmerOperations,
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
        pendingOperations: List<PendingSync>,
        farmerId: String
    ) {

        /*
         * Use the queue snapshot captured before the network request.
         * Do not resolve the current user again during failure handling.
         */
        pendingOperations
            .filter {
                it.entityType ==
                        FARMER_REGISTRATION_ENTITY_TYPE &&
                        it.entityId ==
                        farmerId
            }
            .forEach {
                    pendingOperation ->

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
