package com.beeftech.farmtraceability.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.beeftech.database.entity.Treatment
import com.beeftech.farmtraceability.data.TreatmentRepository
import com.beeftech.farmtraceability.worker.TreatmentSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TreatmentViewModel(
    private val repository: TreatmentRepository,
    private val applicationContext: Context
) : ViewModel() {

    private val _treatments =
        MutableStateFlow<List<Treatment>>(emptyList())

    val treatments: StateFlow<List<Treatment>> =
        _treatments.asStateFlow()

    /*
     * Disease reference data supplied by the backend.
     */
    private val _diseaseOptions =
        MutableStateFlow<List<String>>(
            emptyList()
        )

    val diseaseOptions: StateFlow<List<String>> =
        _diseaseOptions.asStateFlow()

    /*
     * Treatment Type reference data supplied by the backend.
     */
    private val _treatmentOptions =
        MutableStateFlow<List<String>>(
            emptyList()
        )

    val treatmentOptions: StateFlow<List<String>> =
        _treatmentOptions.asStateFlow()

    init {

        /*
         * Load Disease and Treatment Type master/reference
         * data from the backend.
         */
        loadReferenceData()

        /*
         * Safety-net synchronization.
         *
         * WorkManager periodically checks for pending Treatment
         * records whenever network connectivity is available.
         */
        schedulePeriodicSync()
    }

    /*
     * Load the selectable Disease and Treatment Type values
     * from the backend reference-data endpoint.
     */
    fun loadReferenceData() {

        viewModelScope.launch {

            repository
                .loadReferenceData()
                .fold(

                    onSuccess = { referenceData ->

                        _diseaseOptions.value =
                            referenceData.diseases

                        _treatmentOptions.value =
                            referenceData.treatmentTypes
                    },

                    onFailure = {

                        /*
                         * Do not destroy any values that may already
                         * be available if the server cannot currently
                         * be reached.
                         *
                         * A Room-backed reference-data cache can be
                         * added later for full offline support.
                         */
                    }
                )
        }
    }

    fun loadTreatments(
        animalId: String
    ) {

        if (animalId.isBlank()) {
            _treatments.value = emptyList()
            return
        }

        viewModelScope.launch {

            _treatments.value =
                repository.loadTreatments(
                    animalId
                )
        }
    }

    fun saveTreatment(
        animalId: String,
        disease: String,
        treatmentName: String,
        batchNumber: String,
        volumeUsed: String,
        costText: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {

        if (animalId.isBlank()) {
            onResult(
                false,
                "Please select an animal first."
            )
            return
        }

        if (disease.isBlank()) {
            onResult(
                false,
                "Please enter the disease."
            )
            return
        }

        if (treatmentName.isBlank()) {
            onResult(
                false,
                "Please enter the treatment."
            )
            return
        }

        if (batchNumber.isBlank()) {
            onResult(
                false,
                "Please enter the batch number."
            )
            return
        }

        if (volumeUsed.isBlank()) {
            onResult(
                false,
                "Please enter the volume used."
            )
            return
        }

        val cleanedCost =
            costText
                .replace(
                    "R",
                    "",
                    ignoreCase = true
                )
                .replace(
                    " ",
                    ""
                )
                .replace(
                    ",",
                    "."
                )
                .trim()

        val cost =
            cleanedCost.toDoubleOrNull()

        if (
            cost == null ||
            cost < 0
        ) {

            onResult(
                false,
                "Please enter a valid treatment cost."
            )

            return
        }

        viewModelScope.launch {

            val outcome =
                repository.saveTreatment(
                    animalId =
                        animalId,

                    disease =
                        disease,

                    treatmentName =
                        treatmentName,

                    batchNumber =
                        batchNumber,

                    volumeUsed =
                        volumeUsed,

                    cost =
                        cost
                )

            /*
             * Always reload from Room.
             *
             * This keeps Treatment usable while offline.
             */
            _treatments.value =
                repository.loadTreatments(
                    animalId
                )

            val treatment =
                outcome.treatment

            if (treatment == null) {

                onResult(
                    false,
                    outcome.syncErrorMessage
                        ?: "Unable to save treatment record."
                )

                return@launch
            }

            if (outcome.duplicatePrevented) {

                if (
                    treatment.syncStatus ==
                    TreatmentRepository.SYNC_STATUS_SYNCED
                ) {

                    onResult(
                        true,
                        "Treatment already saved. Duplicate prevented."
                    )

                } else {

                    scheduleNetworkAvailableSync()

                    onResult(
                        true,
                        "Treatment already saved locally. Duplicate prevented; pending sync will continue automatically."
                    )
                }

                return@launch
            }

            if (
                treatment.syncStatus ==
                TreatmentRepository.SYNC_STATUS_SYNCED
            ) {

                onResult(
                    true,
                    "Treatment saved and synced successfully."
                )

            } else {

                /*
                 * Record is safely stored in Room.
                 *
                 * WorkManager waits for connectivity and then
                 * retries synchronization automatically.
                 */
                scheduleNetworkAvailableSync()

                onResult(
                    true,
                    "Treatment saved offline. It will sync automatically when internet is available."
                )
            }
        }
    }

    fun retrySync(
        animalId: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {

        viewModelScope.launch {

            val outcome =
                repository.syncPending()

            if (animalId.isNotBlank()) {

                _treatments.value =
                    repository.loadTreatments(
                        animalId
                    )
            }

            if (outcome.syncedCount > 0) {

                val message =
                    if (outcome.syncedCount == 1) {

                        "1 treatment synced successfully."

                    } else {

                        "${outcome.syncedCount} treatments synced successfully."
                    }

                onResult(
                    true,
                    message
                )

            } else if (
                outcome
                    .errorMessagesByRecordGuid
                    .isNotEmpty()
            ) {

                scheduleNetworkAvailableSync()

                val message =
                    outcome
                        .errorMessagesByRecordGuid
                        .values
                        .filterNotNull()
                        .firstOrNull {
                            it.isNotBlank()
                        }
                        ?: "Pending treatments could not be synced."

                onResult(
                    false,
                    message
                )

            } else {

                onResult(
                    true,
                    "There are no pending treatments to sync."
                )
            }
        }
    }

    /*
     * Immediate connectivity-triggered synchronization.
     *
     * If scheduled while offline, WorkManager waits until
     * CONNECTED becomes true.
     */
    private fun scheduleNetworkAvailableSync() {

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

        val request =
            OneTimeWorkRequestBuilder<
                    TreatmentSyncWorker
                    >()
                .setConstraints(
                    constraints
                )
                .build()

        WorkManager
            .getInstance(
                applicationContext
            )
            .enqueueUniqueWork(
                NETWORK_AVAILABLE_SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    /*
     * 15-minute safety net.
     */
    private fun schedulePeriodicSync() {

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

        val request =
            PeriodicWorkRequestBuilder<
                    TreatmentSyncWorker
                    >(
                15,
                TimeUnit.MINUTES
            )
                .setConstraints(
                    constraints
                )
                .build()

        WorkManager
            .getInstance(
                applicationContext
            )
            .enqueueUniquePeriodicWork(
                PERIODIC_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    companion object {

        private const val NETWORK_AVAILABLE_SYNC_WORK_NAME =
            "treatment_network_available_sync"

        private const val PERIODIC_SYNC_WORK_NAME =
            "treatment_periodic_sync"
    }
}