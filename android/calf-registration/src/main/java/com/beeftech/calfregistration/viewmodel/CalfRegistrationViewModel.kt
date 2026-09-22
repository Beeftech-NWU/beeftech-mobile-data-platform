package com.beeftech.calfregistration.viewmodel

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
import com.beeftech.calfregistration.data.CalfRegistrationRepository
import com.beeftech.calfregistration.ui.CalfRegistrationData
import com.beeftech.calfregistration.worker.CalfRegistrationSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class CalfRegistrationViewModel(
    private val repository: CalfRegistrationRepository,
    private val applicationContext: Context
) : ViewModel() {

    private val _registeredCalves =
        MutableStateFlow<List<CalfRegistrationData>>(emptyList())

    val registeredCalves: StateFlow<List<CalfRegistrationData>> =
        _registeredCalves.asStateFlow()

    init {
        loadCalves()

        // Backup periodic sync. This is not the primary sync mechanism.
        schedulePeriodicSync()

        // Also queue an immediate network-constrained check. If the device
        // is offline, WorkManager keeps it waiting until connectivity returns.
        scheduleNetworkAvailableSync()
    }

    fun loadCalves() {
        viewModelScope.launch {
            try {
                _registeredCalves.value = repository.loadAll()
            } catch (_: Exception) {
                _registeredCalves.value = emptyList()
            }
        }
    }

    suspend fun isTagRegistered(tagNumber: String): Boolean {
        return repository.isTagRegistered(tagNumber)
    }

    fun saveCalf(
        formData: CalfRegistrationData,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                /*
                 * saveCalf() already performs an immediate sync attempt.
                 * Therefore:
                 * - online: the record should sync during Save;
                 * - offline: it remains local/pending.
                 */
                val outcome = repository.saveCalf(formData)

                _registeredCalves.value = repository.loadAll()

                if (outcome.data.synced) {
                    onResult(
                        true,
                        "Calf registration saved and synced successfully."
                    )
                } else {
                    /*
                     * Critical offline-first behaviour:
                     * enqueue a ONE-TIME worker requiring connectivity.
                     * If there is no internet now, Android keeps this work
                     * pending and makes it eligible as soon as connectivity
                     * becomes available.
                     */
                    scheduleNetworkAvailableSync()

                    val reason =
                        outcome.syncErrorMessage
                            ?.takeIf { it.isNotBlank() }
                            ?.let { " ($it)" }
                            ?: ""

                    onResult(
                        true,
                        "Calf registration saved locally. Automatic sync is waiting for internet.$reason"
                    )
                }
            } catch (exception: Exception) {
                // Preserve the automatic retry even if the immediate save/sync
                // path reports an exception after local persistence.
                scheduleNetworkAvailableSync()

                onResult(
                    false,
                    exception.message
                        ?: "Unable to save calf registration."
                )
            }
        }
    }

    fun retrySync(
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val outcome = repository.syncPending()

                _registeredCalves.value = repository.loadAll()

                if (outcome.syncedCount > 0) {
                    onResult(
                        true,
                        "${outcome.syncedCount} calf registration" +
                                if (outcome.syncedCount == 1) {
                                    " synced successfully."
                                } else {
                                    "s synced successfully."
                                }
                    )
                } else if (outcome.errorMessagesByAnimalId.isNotEmpty()) {
                    // Manual retry failed, so leave an automatic retry waiting
                    // for a valid network connection.
                    scheduleNetworkAvailableSync()

                    val message =
                        outcome.errorMessagesByAnimalId
                            .values
                            .filterNotNull()
                            .firstOrNull { it.isNotBlank() }
                            ?: "The pending calf registration could not be synced."

                    onResult(false, message)
                } else {
                    onResult(
                        true,
                        "There are no pending calf registrations to sync."
                    )
                }
            } catch (exception: Exception) {
                scheduleNetworkAvailableSync()

                onResult(
                    false,
                    exception.message
                        ?: "Unable to retry calf registration sync."
                )
            }
        }
    }

    /**
     * Primary background retry:
     *
     * This is a one-time request constrained by CONNECTED network state.
     * When created while offline, WorkManager waits until the constraint
     * becomes satisfied instead of waiting for the 15-minute periodic job.
     */
    private fun scheduleNetworkAvailableSync() {
        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val request =
            OneTimeWorkRequestBuilder<CalfRegistrationSyncWorker>()
                .setConstraints(constraints)
                .build()

        WorkManager
            .getInstance(applicationContext)
            .enqueueUniqueWork(
                NETWORK_AVAILABLE_SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    /**
     * Safety-net periodic sync. WorkManager periodic execution is inexact,
     * so this is deliberately not relied upon for the immediate Save path
     * or for the first retry after connectivity returns.
     */
    private fun schedulePeriodicSync() {
        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val request =
            PeriodicWorkRequestBuilder<CalfRegistrationSyncWorker>(
                15,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

        WorkManager
            .getInstance(applicationContext)
            .enqueueUniquePeriodicWork(
                PERIODIC_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    companion object {
        private const val NETWORK_AVAILABLE_SYNC_WORK_NAME =
            "calf-registration-network-available-sync"

        private const val PERIODIC_SYNC_WORK_NAME =
            "calf-registration-periodic-sync"
    }
}
