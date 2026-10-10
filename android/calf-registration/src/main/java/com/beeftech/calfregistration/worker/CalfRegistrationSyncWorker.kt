package com.beeftech.calfregistration.worker

import com.beeftech.database.entity.SyncRunModule
import com.beeftech.database.entity.SyncRunTrigger
import com.beeftech.database.repository.SyncRunRepository
import com.beeftech.database.repository.SyncRunSummary
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.beeftech.calfregistration.data.CalfCaptureContext
import com.beeftech.calfregistration.data.CalfRegistrationApiClient
import com.beeftech.calfregistration.data.CalfRegistrationRepository
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.database.security.TokenProviderRegistry

class CalfRegistrationSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParams
) {

    override suspend fun doWork(): Result {

        val database =
            DatabaseProvider.getDatabase()

        if (database == null) {

            Log.w(
                TAG,
                "Database is not initialized. " +
                    "Calf sync will retry."
            )

            return Result.retry()
        }

        val tokenProvider =
            TokenProviderRegistry.get()

        if (tokenProvider == null) {

            Log.w(
                TAG,
                "Authentication token provider is unavailable. " +
                    "Calf sync will retry."
            )

            return Result.retry()
        }

        return try {

            Log.i(
                TAG,
                "Starting calf synchronization. " +
                    "WorkManager attempt=$runAttemptCount"
            )

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

            val startedAt = System.currentTimeMillis()

            val repository =
                CalfRegistrationRepository(
                    calfRegistrationDao =
                        database.calfRegistrationDao(),

                    pendingSyncRepository =
                        pendingSyncRepository,

                    apiClient =
                        CalfRegistrationApiClient(
                            tokenProvider =
                                tokenProvider
                        ),

                    /*
                     * Worker only synchronizes existing records.
                     * It never creates a calf from this context.
                     */
                    captureContextProvider = {
                        CalfCaptureContext(
                            deviceId = ""
                        )
                    }
                )

            val outcome =
                repository.syncPending()

            /*
             * BEEFTECH_CALF_WORKER_SOURCE_OF_TRUTH
             *
             * Do NOT use only pending_sync to decide whether
             * synchronization succeeded.
             *
             * Query the actual calf-registration records.
             */
            val remainingViews =
                database
                    .calfRegistrationDao()
                    .getPendingRegistrationViews()

            val firstError =
                outcome.errorMessagesByTagNumber.values.firstOrNull()

            val waiting = remainingViews.size

            if (
                SyncRunSummary.worthRecording(
                    trigger,
                    outcome.syncedCount + waiting,
                    waiting,
                    firstError
                )
            ) {
                val summary =
                    SyncRunSummary.of(
                        before = outcome.syncedCount + waiting,
                        after = waiting,
                        error = firstError
                    )

                syncRuns.recordRun(
                    module = SyncRunModule.CALF,
                    trigger = trigger,
                    startedAt = startedAt,
                    syncedCount = outcome.syncedCount,
                    failedCount = maxOf(waiting, outcome.errorMessagesByTagNumber.size),
                    result = summary.result,
                    message = firstError
                )
            }

            if (
                remainingViews.isNotEmpty() ||
                outcome
                    .errorMessagesByTagNumber
                    .isNotEmpty() ||
                outcome.photosPending > 0
            ) {

                Log.w(
                    TAG,
                    "Calf synchronization incomplete. " +
                        "${remainingViews.size} calf(s) remain pending; " +
                        "${outcome.errorMessagesByTagNumber.size} " +
                        "sync error(s). WorkManager will retry."
                )

                Result.retry()

            } else {

                Log.i(
                    TAG,
                    "Calf synchronization completed successfully. " +
                        "${outcome.syncedCount} calf(s) synchronized."
                )

                Result.success()
            }

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Calf synchronization worker failed. " +
                    "Pending records remain safe and will retry.",
                exception
            )

            Result.retry()
        }
    }

    companion object {

        private const val TAG =
            "CalfRegistrationSyncWorker"
    }
}
