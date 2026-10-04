package com.beeftech.management.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beeftech.management.data.Device
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.ManagementResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DevicesUiState(
    val devices: List<Device> = emptyList(),
    /* Null means every phone; otherwise ACTIVE or REVOKED. */
    val status: String? = null,
    val loading: Boolean = false,
    /* True when the last call failed for lack of connection, so the screen can say so. */
    val needsConnection: Boolean = false,
    val error: String? = null,
    val notice: String? = null
)

/*
 * Phones that have signed in. An admin can block and unblock them ([canManage]); a manager sees
 * their own site's phones read-only. The server enforces the same rule.
 */
class DevicesViewModel(
    private val apiClient: ManagementApiClient,
    val canManage: Boolean = false
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevicesUiState())
    val uiState: StateFlow<DevicesUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun selectStatus(status: String?) {
        if (status == _uiState.value.status) return
        _uiState.update { it.copy(status = status) }
        refresh()
    }

    fun refresh() {
        /* A slower answer for an earlier filter must not overwrite the current one. */
        loadJob?.cancel()
        val status = _uiState.value.status
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val result = apiClient.devices(status)
            _uiState.update { state ->
                when (result) {
                    is ManagementResult.Success ->
                        state.copy(devices = result.value, loading = false, needsConnection = false, error = null)
                    else -> failed(state.copy(loading = false), result, LIST_NOT_FOUND)
                }
            }
        }
    }

    fun block(device: Device, reason: String, onDone: () -> Unit = {}) {
        if (!canManage) return
        viewModelScope.launch {
            val result = apiClient.revokeDevice(device.deviceId, reason.trim())
            if (applyDevice(result, "Blocked ${label(device)}")) onDone()
        }
    }

    fun unblock(device: Device, reason: String, onDone: () -> Unit = {}) {
        if (!canManage) return
        viewModelScope.launch {
            val result = apiClient.reinstateDevice(device.deviceId, reason.trim())
            if (applyDevice(result, "Unblocked ${label(device)}")) onDone()
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(notice = null, error = null) }
    }

    /* Replaces the card, or drops it if it no longer matches the status filter. Returns true on success. */
    private fun applyDevice(result: ManagementResult<Device>, notice: String): Boolean {
        _uiState.update { state ->
            if (result is ManagementResult.Success) {
                val updated = result.value
                val matches = state.status == null || state.status == updated.status
                state.copy(
                    devices = if (matches) {
                        state.devices.map { if (it.deviceId == updated.deviceId) updated else it }
                    } else {
                        state.devices.filterNot { it.deviceId == updated.deviceId }
                    },
                    notice = notice,
                    error = null,
                    needsConnection = false
                )
            } else {
                failed(state, result)
            }
        }
        return result is ManagementResult.Success
    }

    private fun label(device: Device) = device.model ?: device.deviceId

    /*
     * A 404 on one phone means it no longer exists; on the list it means the server has no
     * devices endpoint (e.g. an older backend). A 409 (already blocked / not blocked) arrives
     * as Rejected with the server's own message.
     */
    private fun failed(
        state: DevicesUiState,
        result: ManagementResult<*>,
        notFoundMessage: String = DEVICE_NOT_FOUND
    ): DevicesUiState =
        when (result) {
            is ManagementResult.NoConnection -> state.copy(needsConnection = true, error = null)
            is ManagementResult.Unauthorized ->
                state.copy(error = "Your session has expired. Log out and sign in again.")
            is ManagementResult.Forbidden -> state.copy(error = result.message)
            is ManagementResult.NotFound -> state.copy(error = notFoundMessage)
            is ManagementResult.Rejected -> state.copy(error = result.message)
            is ManagementResult.Error -> state.copy(error = result.message)
            is ManagementResult.Success -> state
        }

    private companion object {
        const val DEVICE_NOT_FOUND = "That phone no longer exists."
        const val LIST_NOT_FOUND = "The server doesn't support phones yet. Update the server and try again."
    }
}

class DevicesViewModelFactory(
    private val apiClient: ManagementApiClient,
    private val canManage: Boolean = false
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = DevicesViewModel(apiClient, canManage) as T
}
