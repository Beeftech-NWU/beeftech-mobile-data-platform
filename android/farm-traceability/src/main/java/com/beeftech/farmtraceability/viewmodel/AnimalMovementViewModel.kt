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
import com.beeftech.database.entity.AnimalMovement
import com.beeftech.farmtraceability.data.AnimalMovementRepository
import com.beeftech.farmtraceability.worker.AnimalMovementSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AnimalMovementViewModel(
    private val repository: AnimalMovementRepository,
    private val applicationContext: Context
) : ViewModel() {

    private val _movements =
        MutableStateFlow<List<AnimalMovement>>(emptyList())

    val movements: StateFlow<List<AnimalMovement>> =
        _movements.asStateFlow()

    init {
        /*
         * Safety-net synchronization.
         *
         * WorkManager will periodically check for pending movement
         * records whenever network connectivity is available.
         */
        schedulePeriodicSync()
    }

    fun loadMovements(
        animalId: String
    ) {

        if (animalId.isBlank()) {
            _movements.value = emptyList()
            return
        }

        viewModelScope.launch {

            _movements.value =
                repository.loadMovements(
                    animalId
                )
        }
    }

    fun saveMovement(
        animalId: String,
        movementInformation: String,
        responsibleWorker: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {

        if (animalId.isBlank()) {
            onResult(
                false,
                "Please select an animal first."
            )
            return
        }

        if (movementInformation.isBlank()) {
            onResult(
                false,
                "Please enter movement information."
            )
            return
        }

        if (responsibleWorker.isBlank()) {
            onResult(
                false,
                "Please enter the responsible worker."
            )
            return
        }

        viewModelScope.launch {

            val outcome =
                repository.saveMovement(
                    animalId = animalId,
                    movementInformation = movementInformation,
                    responsibleWorker = responsibleWorker
                )

            /*
             * Always reload from Room.
             *
             * This means the UI continues working even when
             * the device has no internet connection.
             */
            _movements.value =
                repository.loadMovements(
                    animalId
                )

            val movement =
                outcome.movement

            if (movement == null) {

                onResult(
                    false,
                    outcome.syncErrorMessage
                        ?: "Unable to save movement record."
                )

                return@launch
            }

            if (
                movement.syncStatus ==
                AnimalMovementRepository.SYNC_STATUS_SYNCED
            ) {

                onResult(
                    true,
                    "Movement saved and synced successfully."
                )

            } else {

                /*
                 * The record is safely stored locally.
                 *
                 * Schedule a network-constrained worker. If the
                 * device is offline, WorkManager waits. When
                 * connectivity becomes available, Android can
                 * execute the worker automatically.
                 */
                scheduleNetworkAvailableSync()

                onResult(
                    true,
                    "Movement saved offline. It will sync automatically when internet is available."
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

                _movements.value =
                    repository.loadMovements(
                        animalId
                    )
            }

            if (outcome.syncedCount > 0) {

                val message =
                    if (outcome.syncedCount == 1) {
                        "1 movement synced successfully."
                    } else {
                        "${outcome.syncedCount} movements synced successfully."
                    }

                onResult(
                    true,
                    message
                )

            } else if (
                outcome.errorMessagesByRecordGuid.isNotEmpty()
            ) {

                /*
                 * Keep an automatic retry waiting for connectivity.
                 */
                scheduleNetworkAvailableSync()

                val message =
                    outcome
                        .errorMessagesByRecordGuid
                        .values
                        .filterNotNull()
                        .firstOrNull {
                            it.isNotBlank()
                        }
                        ?: "Pending movements could not be synced."

                onResult(
                    false,
                    message
                )

            } else {

                onResult(
                    true,
                    "There are no pending movements to sync."
                )
            }
        }
    }

    /*
     * Immediate connectivity-triggered synchronization.
     *
     * If this is scheduled while offline, WorkManager waits
     * until CONNECTED becomes true.
     */
    private fun scheduleNetworkAvailableSync() {

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

        val request =
            OneTimeWorkRequestBuilder<AnimalMovementSyncWorker>()
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
     *
     * This is not relied upon for the first synchronization
     * after connectivity returns. The one-time worker above
     * handles that case.
     */
    private fun schedulePeriodicSync() {

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

        val request =
            PeriodicWorkRequestBuilder<AnimalMovementSyncWorker>(
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
            "animal_movement_network_available_sync"

        private const val PERIODIC_SYNC_WORK_NAME =
            "animal_movement_periodic_sync"
    }
}