package com.beeftech.management.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.ManagementResult
import com.beeftech.management.data.SyncPolicyDto
import com.beeftech.management.data.SyncPolicyRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SyncPolicyUiState(
    /* What the server has now; null until the first load. */
    val saved: SyncPolicyDto? = null,
    /* The form: kept as text so a half-typed number is not lost. */
    val day1: String = "",
    val day2: String = "",
    val day3: String = "",
    val staleHours: String = "",
    val loading: Boolean = false,
    val saving: Boolean = false,
    /* True when the last call failed for lack of connection, so the screen can say so. */
    val needsConnection: Boolean = false,
    val error: String? = null,
    val notice: String? = null
) {
    private val days: List<Int?> get() = listOf(day1, day2, day3).map { it.trim().toIntOrNull() }
    private val hours: Int? get() = staleHours.trim().toIntOrNull()

    /* What's wrong with the form, or null. Empty until something has loaded. */
    val validationError: String?
        get() = if (saved == null) null
        else SyncPolicyRules.warningDaysError(days) ?: SyncPolicyRules.staleHoursError(hours)

    val changed: Boolean
        get() = saved != null && (days != saved.warningDays || hours != saved.staleSyncAlertHours)

    val canSave: Boolean get() = saved != null && !saving && changed && validationError == null
}

/*
 * Admin screen for the sync policy: the three warning days and the dashboard's stale-sync alert.
 * The wipe day is shown but not editable; it is fixed at 7 in the app.
 */
class SyncPolicyViewModel(
    private val apiClient: ManagementApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncPolicyUiState())
    val uiState: StateFlow<SyncPolicyUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val result = apiClient.syncPolicy()
            _uiState.update { state ->
                when (result) {
                    is ManagementResult.Success ->
                        result.value.toForm(state.copy(loading = false, needsConnection = false, error = null))
                    else -> failed(state.copy(loading = false), result, LOAD_NOT_FOUND)
                }
            }
        }
    }

    fun setDay(index: Int, text: String) {
        val clean = text.filter { it.isDigit() }.take(2)
        _uiState.update {
            when (index) {
                0 -> it.copy(day1 = clean, notice = null)
                1 -> it.copy(day2 = clean, notice = null)
                else -> it.copy(day3 = clean, notice = null)
            }
        }
    }

    fun setStaleHours(text: String) {
        _uiState.update { it.copy(staleHours = text.filter { c -> c.isDigit() }.take(3), notice = null) }
    }

    /* Puts the form back to what the server has. */
    fun reset() {
        _uiState.update { state -> state.saved?.toForm(state.copy(error = null, notice = null)) ?: state }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        val days = listOf(state.day1, state.day2, state.day3).map { it.trim().toInt() }
        val hours = state.staleHours.trim().toInt()

        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = apiClient.saveSyncPolicy(days, hours)
            _uiState.update { current ->
                if (result is ManagementResult.Success) {
                    result.value.toForm(
                        current.copy(saving = false, notice = "Saved. Phones pick it up the next time they connect.", error = null, needsConnection = false)
                    )
                } else {
                    failed(current.copy(saving = false), result)
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(notice = null, error = null) }
    }

    private fun SyncPolicyDto.toForm(base: SyncPolicyUiState) = base.copy(
        saved = this,
        day1 = warningDays.getOrNull(0)?.toString().orEmpty(),
        day2 = warningDays.getOrNull(1)?.toString().orEmpty(),
        day3 = warningDays.getOrNull(2)?.toString().orEmpty(),
        staleHours = staleSyncAlertHours.toString()
    )

    /* A 400 (the server's rules) arrives as Rejected with its own message. */
    private fun failed(
        state: SyncPolicyUiState,
        result: ManagementResult<*>,
        notFoundMessage: String = SAVE_NOT_FOUND
    ): SyncPolicyUiState =
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
        const val LOAD_NOT_FOUND = "The server doesn't support the sync policy yet. Update the server and try again."
        const val SAVE_NOT_FOUND = "The server doesn't support the sync policy yet."
    }
}

class SyncPolicyViewModelFactory(
    private val apiClient: ManagementApiClient
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SyncPolicyViewModel(apiClient) as T
}
