package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeftech.farmtraceability.data.MortalityRepository
import com.beeftech.database.entity.Mortality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MortalityViewModel(
    private val repository: MortalityRepository
) : ViewModel() {

    private val _mortalities =
        MutableStateFlow<List<Mortality>>(emptyList())

    val mortalities: StateFlow<List<Mortality>> =
        _mortalities.asStateFlow()

    fun loadMortalities(
        animalId: String
    ) {

        if (animalId.isBlank()) {
            _mortalities.value = emptyList()
            return
        }

        viewModelScope.launch {

            try {

                _mortalities.value =
                    repository.loadMortalities(
                        animalId
                    )

            } catch (exception: Exception) {

                _mortalities.value =
                    emptyList()
            }
        }
    }

    fun saveMortality(
        animalId: String,
        mortalityReason: String,
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

        if (mortalityReason.isBlank()) {

            onResult(
                false,
                "Please enter the mortality reason."
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

            try {

                val outcome =
                    repository.saveMortality(
                        animalId = animalId,
                        causeOfDeath = mortalityReason,
                        responsibleWorker = responsibleWorker
                    )

                _mortalities.value =
                    repository.loadMortalities(
                        animalId
                    )

                /*
                 * The record is saved either way; a sync problem only
                 * means it is waiting for a connection.
                 */
                onResult(
                    true,
                    if (outcome.syncErrorMessage == null) {
                        "Mortality record saved successfully."
                    } else {
                        "Mortality record saved. It will sync when a connection is available."
                    }
                )

            } catch (exception: Exception) {

                onResult(
                    false,
                    "Unable to save mortality record."
                )
            }
        }
    }

    fun retrySync(
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {

        viewModelScope.launch {

            val outcome = repository.syncPending()

            val failed = outcome.errorMessagesByRecordGuid.isNotEmpty()

            onResult(
                !failed,
                when {
                    failed -> "Mortality sync failed. We'll try again later."
                    outcome.syncedCount > 0 -> "Mortality records synced successfully."
                    else -> "No mortality records to sync."
                }
            )
        }
    }
}
