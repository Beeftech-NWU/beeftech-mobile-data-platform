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
import com.beeftech.database.repository.SyncPolicyStore
import com.beeftech.database.security.CurrentUserIdRegistry
import com.beeftech.database.security.TokenProviderRegistry
import kotlinx.coroutines.CancellationException
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.PolicySyncOutcome
import com.beeftech.management.data.ReferenceDataSync
import com.beeftech.management.data.EventUploadOutcome
import com.beeftech.management.data.ReferenceSyncOutcome
import com.beeftech.management.data.SecurityEventSync
import com.beeftech.management.data.SyncLockRelease
import com.beeftech.management.data.SyncPolicySync

/**
 * Talks to the server on the device's behalf: pulls the reference data (disease, treatment type and
 * cost type lists) and the sync-warning policy (when phones warn about unsynced data), uploads the
 * security events the phone recorded (warnings, the Day-7 wipe and lock), and lifts the phone's
 * Day-7 lock once an admin has cleared it on the server.
 *
 * It runs right after an online sign-in, including for a locked account: after a wipe that is the
 * only time a valid token exists, so it is the only chance to report the lock and hear it cleared.
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

            val apiClient =
                ManagementApiClient(tokenProvider = tokenProvider)

            /* Independent steps: one failing doesn't stop the other. */
            val referenceData =
                runStep("Reference data") {
                    ReferenceDataSync(
                        apiClient = apiClient,
                        dao = database.referenceDataDao()
                    ).pull() !is ReferenceSyncOutcome.Failed
                }

            /* Without a signed-in user there is nobody whose events or lock to handle. */
            val userId = CurrentUserIdRegistry.currentUserId()

            /* Before the policy pull, so the server hears about a lock before we ask whether it was cleared. */
            val events =
                if (userId == null) true
                else runStep("Security events") {
                    SecurityEventSync(
                        apiClient = apiClient,
                        dao = database.syncSecurityDao(),
                        userId = userId
                    ).upload() !is EventUploadOutcome.Failed
                }

            val syncPolicy =
                runStep("Sync policy") {
                    val release = userId?.let { SyncLockRelease(database.syncSecurityDao(), it) }

                    SyncPolicySync(
                        apiClient = apiClient,
                        store = SyncPolicyStore(database.referenceDataDao()),
                        onLockClearedAt = { clearedAt ->
                            if (release?.apply(clearedAt) == true) Log.i(TAG, "Day-7 lock cleared by an administrator.")
                        }
                    ).pull() !is PolicySyncOutcome.Failed
                }

            if (referenceData && events && syncPolicy) Result.success() else Result.retry()

        } catch (exception: Exception) {

            Log.e(TAG, "Device check-in failed.", exception)

            Result.retry()
        }
    }

    /* True if the step worked. An error in one step is logged and counted as a failure of that step only. */
    private suspend fun runStep(name: String, step: suspend () -> Boolean): Boolean =
        try {
            step().also { Log.i(TAG, "$name: ${if (it) "ok" else "will retry"}") }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "$name failed.", exception)
            false
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
