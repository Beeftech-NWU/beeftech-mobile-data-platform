package com.beeftech.demoapp

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.beeftech.calfregistration.worker.CalfRegistrationSyncWorker
import com.beeftech.database.repository.SyncRunSummary
import com.beeftech.farmerregistration.worker.FarmerSyncWorker
import com.beeftech.farmtraceability.worker.AnimalMovementSyncWorker
import com.beeftech.farmtraceability.worker.CostSyncWorker
import com.beeftech.farmtraceability.worker.MortalitySyncWorker
import com.beeftech.farmtraceability.worker.TraceabilityOutboxWorker
import com.beeftech.farmtraceability.worker.TreatmentSyncWorker

/**
 * Starts every module's sync worker, tagging each with what asked for it (the morning or evening
 * window, the Sync now button, or nothing special) so the sync history can say so.
 *
 * The scheduled windows use [ExistingWorkPolicy.KEEP], so they never start a second copy of a
 * worker that is already syncing. Sync now uses [ExistingWorkPolicy.REPLACE]: it is an explicit
 * request, so it restarts the worker instead of waiting for it.
 */
object SyncAllDispatcher {

    /** Unique work names. The first five match the queues the modules already use on their own. */
    internal val WORK_NAMES = listOf(
        "farmer-registration-sync",
        "calf-registration-network-available-sync",
        "treatment_network_available_sync",
        "animal-movement-scheduled-sync",
        "mortality-scheduled-sync",
        "cost-scheduled-sync",
        "traceability-outbox-scheduled-sync"
    )

    fun dispatch(context: Context, trigger: String, policy: ExistingWorkPolicy) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        val input = triggerData(trigger)

        listOf(
            WORK_NAMES[0] to request<FarmerSyncWorker>(input),
            WORK_NAMES[1] to request<CalfRegistrationSyncWorker>(input),
            WORK_NAMES[2] to request<TreatmentSyncWorker>(input),
            WORK_NAMES[3] to request<AnimalMovementSyncWorker>(input),
            WORK_NAMES[4] to request<MortalitySyncWorker>(input),
            WORK_NAMES[5] to request<CostSyncWorker>(input),
            WORK_NAMES[6] to request<TraceabilityOutboxWorker>(input)
        ).forEach { (name, request) ->
            workManager.enqueueUniqueWork(name, policy, request)
        }
    }

    internal fun triggerData(trigger: String): Data =
        workDataOf(SyncRunSummary.TRIGGER_INPUT_KEY to trigger)

    /* Each child needs a connection; WorkManager holds it until one is available. */
    private inline fun <reified T : ListenableWorker> request(input: Data): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<T>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(input)
            .build()
}
