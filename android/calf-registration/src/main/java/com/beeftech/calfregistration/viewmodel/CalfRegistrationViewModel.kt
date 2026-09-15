package com.beeftech.calfregistration.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeftech.calfregistration.data.CalfRegistrationRepository
import com.beeftech.calfregistration.ui.CalfRegistrationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CalfRegistrationViewModel(
    private val repository: CalfRegistrationRepository
) : ViewModel() {

    private val _registeredCalves =
        MutableStateFlow<List<CalfRegistrationData>>(emptyList())

    val registeredCalves: StateFlow<List<CalfRegistrationData>> =
        _registeredCalves.asStateFlow()

    init {
        loadCalves()
    }

    fun loadCalves() {
        viewModelScope.launch {
            try {
                _registeredCalves.value = repository.loadAll()
            } catch (exception: Exception) {
                _registeredCalves.value = emptyList()
            }
        }
    }

    fun saveCalf(
        formData: CalfRegistrationData,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val outcome = repository.saveCalf(formData)

                _registeredCalves.value = repository.loadAll()

                if (outcome.data.synced) {
                    onResult(true, "Calf registration saved and synced successfully.")
                } else {
                    val reason = outcome.syncErrorMessage?.let { " ($it)" } ?: ""
                    onResult(true, "Calf registration saved locally. Will sync when possible.$reason")
                }
            } catch (exception: Exception) {
                onResult(false, "Unable to save calf registration.")
            }
        }
    }

    fun retrySync() {
        viewModelScope.launch {
            try {
                repository.syncPending()
            } catch (exception: Exception) {
                // Leave records as PENDING for a later retry.
            }

            loadCalves()
        }
    }
}
