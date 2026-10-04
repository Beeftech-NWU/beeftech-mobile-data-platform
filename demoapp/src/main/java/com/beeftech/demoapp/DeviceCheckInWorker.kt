package com.beeftech.demoapp

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.security.TokenProviderRegistry
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.ReferenceDataSync
import com.beeftech.management.data.ReferenceSyncOutcome

/**
 * Pulls what the server publishes for the device: today the reference data (disease, treatment
 * type and cost type lists), later the sync-warning policy.
 *
 * It never touches the records queued for upload and never uses the pending_sync queue, so it can't
 * block or reorder a sync. Each step is independent: one failing leaves the others to run, and the
 * whole job is retried later.
 */
class DeviceCheckInWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParams
) {

    override suspend fun doWork(): Result {

        val database =
            DatabaseProvider.getDatabase()
                ?: return Result.failure()

        val tokenProvider =
            TokenProviderRegistry.get()
                ?: return Result.retry()

        /*
         * Offline login grants local access only, so there may be no valid server token.
         * Wait for an online sign-in rather than counting this as a failure.
         */
        if (tokenProvider.token() == null) {
            return Result.retry()
        }

        return try {

            val referenceData =
                ReferenceDataSync(
                    apiClient = ManagementApiClient(tokenProvider = tokenProvider),
                    dao = database.referenceDataDao()
                ).pull()

            Log.i(TAG, "Reference data: $referenceData")

            if (referenceData is ReferenceSyncOutcome.Failed) Result.retry() else Result.success()

        } catch (exception: Exception) {

            Log.e(TAG, "Device check-in failed.", exception)

            Result.retry()
        }
    }

    companion object {

        private const val TAG = "DeviceCheckIn"

        private const val WORK_NAME = "device-check-in"

        /* KEEP, so asking twice while one is waiting or running doesn't start a second. */
        fun enqueue(context: Context) {

            val request =
                OneTimeWorkRequestBuilder<DeviceCheckInWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()

            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
