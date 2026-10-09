package com.beeftech.demoapp

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.beeftech.calfregistration.worker.CalfRegistrationSyncWorker
import com.beeftech.farmerregistration.worker.FarmerSyncWorker
import com.beeftech.farmtraceability.worker.AnimalMovementSyncWorker
import com.beeftech.farmtraceability.worker.FarmerAnimalLinkSyncWorker
import com.beeftech.farmtraceability.worker.CostSyncWorker
import com.beeftech.farmtraceability.worker.MortalitySyncWorker
import com.beeftech.farmtraceability.worker.TreatmentSyncWorker

class ScheduledBatchSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParams
) {

    override suspend fun doWork(): Result {

        return try {

            val workManager =
                WorkManager.getInstance(
                    applicationContext
                )

            /*
             * Reuse the existing module workers.
             *
             * Each child job requires connectivity. If the scheduled
             * window arrives while the device is offline, WorkManager
             * keeps that child request waiting until connectivity
             * becomes available.
             */

            workManager.enqueueUniqueWork(
                FARMER_SYNC_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                connectedWorkRequest<
                    FarmerSyncWorker
                >()
            )

            workManager.enqueueUniqueWork(
                CALF_SYNC_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                connectedWorkRequest<
                    CalfRegistrationSyncWorker
                >()
            )

            workManager.enqueueUniqueWork(
                FARMER_ANIMAL_SYNC_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                connectedWorkRequest<FarmerAnimalLinkSyncWorker>()
            )

            workManager.enqueueUniqueWork(
                TREATMENT_SYNC_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                connectedWorkRequest<
                    TreatmentSyncWorker
                >()
            )

            workManager.enqueueUniqueWork(
                ANIMAL_MOVEMENT_SYNC_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                connectedWorkRequest<
                    AnimalMovementSyncWorker
                >()
            )

            workManager.enqueueUniqueWork(
                MORTALITY_SYNC_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                connectedWorkRequest<
                    MortalitySyncWorker
                >()
            )

            workManager.enqueueUniqueWork(
                COST_SYNC_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                connectedWorkRequest<
                    CostSyncWorker
                >()
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

    private inline fun <
        reified T : ListenableWorker
    > connectedWorkRequest():
        OneTimeWorkRequest {

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

        return OneTimeWorkRequestBuilder<T>()
            .setConstraints(
                constraints
            )
            .build()
    }

    companion object {

        private const val TAG =
            "ScheduledBatchSync"

        /*
         * Farmer, calf and treatment names deliberately match
         * their existing immediate/network-recovery queues.
         *
         * This prevents the scheduled job from unnecessarily
         * starting a duplicate worker if that module is already
         * synchronising.
         */

        private const val FARMER_SYNC_WORK_NAME =
            "farmer-registration-sync"

        private const val CALF_SYNC_WORK_NAME =
            "calf-registration-network-available-sync"

        private const val FARMER_ANIMAL_SYNC_WORK_NAME =
            "farmer-animal-link-sync"

        private const val TREATMENT_SYNC_WORK_NAME =
            "treatment_network_available_sync"

        private const val ANIMAL_MOVEMENT_SYNC_WORK_NAME =
            "animal-movement-scheduled-sync"

        private const val MORTALITY_SYNC_WORK_NAME =
            "mortality-scheduled-sync"

        private const val COST_SYNC_WORK_NAME =
            "cost-scheduled-sync"
    }
}