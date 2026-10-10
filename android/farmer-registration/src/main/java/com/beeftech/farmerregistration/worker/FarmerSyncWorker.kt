package com.beeftech.farmerregistration.worker

import com.beeftech.database.entity.SyncRunModule
import com.beeftech.database.entity.SyncRunTrigger
import com.beeftech.database.repository.SyncRunRepository
import com.beeftech.database.repository.SyncRunSummary
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.entity.PendingSync
import com.beeftech.database.repository.FarmerRepository
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.database.repository.SyncRepository
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

        Log.i(
            TAG,
            "Farmer synchronization worker started. " +
                "WorkManager attempt=$runAttemptCount"
        )

        return try {

            val database =
                DatabaseProvider.getDatabase()

            if (database == null) {

                Log.e(
                    TAG,
                    "Farmer synchronization cannot start: " +
                        "database is not initialized."
                )

                return Result.retry()
            }

            val tokenProvider =
                TokenProviderRegistry.get()

            if (tokenProvider == null) {

                Log.e(
                    TAG,
                    "Farmer synchronization cannot start: " +
                        "TokenProviderRegistry is empty."
                )

                return Result.retry()
            }

            /*
             * BEEFTECH_VALID_SERVER_TOKEN_GUARD
             *
             * A user may be allowed to work locally for up to
             * seven days while offline, but that does not mean
             * a valid Render JWT exists.
             *
             * Do not consume the Farmer Registration retry
             * counter when authentication is unavailable.
             */
            if (tokenProvider.token() == null) {

                Log.w(
                    "FarmerSyncWorker",
                    "No valid Render authentication token. " +
                        "Farmer registrations remain safely queued. " +
                        "An online sign-in is required before synchronization."
                )

                SyncRunRepository(
                    database.syncRunDao(),
                    database.pendingSyncDao()
                ).recordOffline(
                    module = SyncRunModule.FARMER,
                    trigger =
                        inputData.getString(SyncRunSummary.TRIGGER_INPUT_KEY)
                            ?: SyncRunTrigger.AUTO,
                    firstAttempt = runAttemptCount == 0
                )

                return Result.retry()
            }

            val farmerRepository =
                FarmerRepository(
                    database.farmerDao()
                )

            val pendingSyncRepository =
                PendingSyncRepository(
                    database.pendingSyncDao()
                )

            /*
             * BEEFTECH_FARMER_LAST_SYNC_HISTORY
             *
             * The Farm Traceability screen reads Last Sync from
             * the sync_batch table. Farmer registration sync must
             * therefore record successful synchronization there.
             */
            val syncRepository =
                SyncRepository(
                    pendingSyncDao =
                        database.pendingSyncDao(),

                    syncBatchDao =
                        database.syncBatchDao()
                )

            val apiClient =
                FarmerApiClient(
                    context = applicationContext,
                    tokenProvider = tokenProvider
                )

            val syncRuns =
                SyncRunRepository(
                    database.syncRunDao(),
                    database.pendingSyncDao()
                )

            val trigger =
                inputData.getString(SyncRunSummary.TRIGGER_INPUT_KEY)
                    ?: SyncRunTrigger.AUTO

            val startedAt = System.currentTimeMillis()

            /*
             * pending_sync is the ownership boundary.
             *
             * Only Farmer Registration entries owned by the currently
             * authenticated user may be synchronized.
             *
             * Automatic synchronization is capped at MAX_RETRY_COUNT.
             * Manual Retry Sync resets the selected queue rows to zero
             * before this worker is scheduled.
             */
            val allFarmerOperations =
                pendingSyncRepository
                    .getAllPendingOperations()
                    .filter {
                        it.entityType ==
                            FARMER_REGISTRATION_ENTITY_TYPE
                    }

            Log.i(
                TAG,
                "Farmer queue contains " +
                    "${allFarmerOperations.size} Farmer Registration operation(s)."
            )

            allFarmerOperations.forEach { operation ->

                Log.i(
                    TAG,
                    "Queue item id=${operation.id}, " +
                        "farmerId=${operation.entityId}, " +
                        "retryCount=${operation.retryCount}"
                )
            }

            val farmerOperations =
                allFarmerOperations
                    .filter {
                        it.retryCount <
                            MAX_RETRY_COUNT
                    }

            if (farmerOperations.isEmpty()) {

                if (allFarmerOperations.isNotEmpty()) {

                    Log.w(
                        TAG,
                        "No Farmer Registration records are retry-eligible. " +
                            "They have reached the retry limit. " +
                            "Use Retry Sync to reset their retry counters."
                    )

                } else {

                    Log.i(
                        TAG,
                        "There are no Farmer Registration records waiting to sync."
                    )
                }

                return Result.success()
            }

            val queuedFarmers =
                farmerOperations
                    .map { pendingOperation ->

                        pendingOperation to
                            farmerRepository
                                .getFarmerById(
                                    pendingOperation.entityId
                                )
                    }

            /*
             * Remove stale queue rows for farmers which are already
             * synchronized or which no longer exist locally.
             */
            queuedFarmers
                .filter { (_, farmer) ->

                    farmer == null ||
                        farmer.sync_status
                            .equals(
                                "SYNCED",
                                ignoreCase = true
                            )
                }
                .forEach { (pendingOperation, farmer) ->

                    Log.i(
                        TAG,
                        if (farmer == null) {
                            "Removing stale Farmer queue row " +
                                "${pendingOperation.id}: local Farmer no longer exists."
                        } else {
                            "Removing stale Farmer queue row " +
                                "${pendingOperation.id}: Farmer is already SYNCED."
                        }
                    )

                    pendingSyncRepository
                        .markSyncSuccessful(
                            pendingOperation.id
                        )
                }

            val pendingFarmers =
                queuedFarmers
                    .mapNotNull { (_, farmer) ->

                        farmer
                            ?.takeIf {
                                it.sync_status
                                    .equals(
                                        "PENDING",
                                        ignoreCase = true
                                    ) ||
                                    it.sync_status
                                        .equals(
                                            "PROCESSING",
                                            ignoreCase = true
                                        )
                            }
                    }
                    .distinctBy {
                        it.farmer_id
                    }

            if (pendingFarmers.isEmpty()) {

                Log.i(
                    TAG,
                    "No pending Farmer records remain after stale queue cleanup."
                )

                return Result.success()
            }

            var hasFailure = false
            var hasSuccessfulSync = false
            var syncedCount = 0
            var failedCount = 0

            pendingFarmers.forEach { farmer ->

                val farmerId =
                    farmer.farmer_id

                Log.i(
                    TAG,
                    "Starting Farmer sync: " +
                        "farmerId=$farmerId, " +
                        "clientCode=${farmer.client_code}"
                )

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

                    Log.i(
                        TAG,
                        "Sending Farmer $farmerId to Render API. " +
                            "addresses=${addresses.size}, " +
                            "roles=${roles.size}"
                    )

                    val syncResult =
                        apiClient.syncFarmer(
                            farmer = farmer,
                            addresses = addresses,
                            roles = roles
                        )

                    val apiResult =
                        syncResult.getOrNull()

                    val apiException =
                        syncResult.exceptionOrNull()

                    if (apiException != null) {

                        Log.e(
                            TAG,
                            "Farmer $farmerId API request failed: " +
                                (
                                    apiException.message
                                        ?: apiException::class.java.simpleName
                                ),
                            apiException
                        )
                    }

                    if (apiResult != null) {

                        Log.i(
                            TAG,
                            "Farmer $farmerId server response: " +
                                "status=${apiResult.status}, " +
                                "message=${apiResult.message}, " +
                                "serverSyncedAt=${apiResult.serverSyncedAt}"
                        )
                    }

                    val syncSuccessful =
                        apiResult
                            ?.status
                            ?.equals(
                                "SYNCED",
                                ignoreCase = true
                            ) == true

                    if (syncSuccessful) {

                        hasSuccessfulSync = true

                        syncedCount++

                        farmerRepository
                            .markAsSynced(
                                farmerId
                            )

                        farmerOperations
                            .filter {
                                it.entityId ==
                                    farmerId
                            }
                            .forEach { pendingOperation ->

                                pendingSyncRepository
                                    .markSyncSuccessful(
                                        pendingOperation.id
                                    )
                            }

                        Log.i(
                            TAG,
                            "Farmer $farmerId synchronized successfully. " +
                                "Local status is now SYNCED and its queue entry was removed."
                        )

                    } else {

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

                        Log.e(
                            TAG,
                            "Farmer $farmerId remains PENDING. " +
                                "Server status=${apiResult?.status ?: "NO_RESPONSE"}, " +
                                "reason=${
                                    apiResult?.message
                                        ?: apiException?.message
                                        ?: "No failure message returned"
                                }"
                        )

                        hasFailure = true

                        failedCount++
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

                    Log.e(
                        TAG,
                        "Unexpected Farmer sync failure for $farmerId: " +
                            (
                                exception.message
                                    ?: exception::class.java.simpleName
                            ),
                        exception
                    )

                    hasFailure = true

                    failedCount++
                }
            }

            /*
             * Store one successful-sync timestamp for the complete
             * worker run. The UI observes this table reactively, so
             * Last Sync will update automatically.
             */
            if (hasSuccessfulSync) {

                syncRepository
                    .recordSuccessfulSync()

                Log.i(
                    "FarmerSyncWorker",
                    "Successful Farmer synchronization recorded " +
                        "for Farm Traceability Last Sync."
                )
            }

            if (SyncRunSummary.worthRecording(trigger, syncedCount + failedCount, failedCount, null)) {
                val summary =
                    SyncRunSummary.of(
                        before = syncedCount + failedCount,
                        after = failedCount
                    )

                syncRuns.recordRun(
                    module = SyncRunModule.FARMER,
                    trigger = trigger,
                    startedAt = startedAt,
                    syncedCount = syncedCount,
                    failedCount = failedCount,
                    result = summary.result,
                    message = if (failedCount > 0) "$failedCount farmer registration(s) could not be sent." else null
                )
            }

            if (hasFailure) {

                Log.w(
                    TAG,
                    "Farmer synchronization completed with failure(s). " +
                        "WorkManager will retry."
                )

                Result.retry()

            } else {

                Log.i(
                    TAG,
                    "Farmer synchronization completed successfully."
                )

                Result.success()
            }

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "FarmerSyncWorker failed before synchronization completed.",
                exception
            )

            Result.retry()
        }
    }

    private suspend fun markFarmerSyncFailed(
        pendingSyncRepository: PendingSyncRepository,
        pendingOperations: List<PendingSync>,
        farmerId: String
    ) {

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

        private const val TAG =
            "FarmerSyncWorker"

        private const val FARMER_REGISTRATION_ENTITY_TYPE =
            "FARMER_REGISTRATION"

        private const val MAX_RETRY_COUNT =
            5
    }
}
