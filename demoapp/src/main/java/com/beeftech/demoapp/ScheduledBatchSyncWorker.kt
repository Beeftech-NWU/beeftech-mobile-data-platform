package com.beeftech.demoapp

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkerParameters
import com.beeftech.database.entity.SyncRunTrigger
import com.beeftech.database.repository.SyncRunSummary

class ScheduledBatchSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParams
) {

    override suspend fun doWork(): Result {

        return try {

            /*
             * Reuse the existing module workers, now through one dispatcher.
             *
             * Each child job requires connectivity. If the scheduled
             * window arrives while the device is offline, WorkManager
             * keeps that child request waiting until connectivity
             * becomes available.
             */
            SyncAllDispatcher.dispatch(
                applicationContext,
                inputData.getString(SyncRunSummary.TRIGGER_INPUT_KEY) ?: SyncRunTrigger.AUTO,
                ExistingWorkPolicy.KEEP
            )

            /* Not a sync: pulls reference data (and later the sync policy) from the server. */
            DeviceCheckInWorker.enqueue(applicationContext)

            Log.i(
                TAG,
                "Scheduled BeefTech batch sync dispatched."
            )

            Result.success()

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Unable to dispatch scheduled BeefTech sync.",
                exception
            )

            Result.retry()
        }
    }

    private companion object {

        private const val TAG =
            "ScheduledBatchSync"
    }
}
