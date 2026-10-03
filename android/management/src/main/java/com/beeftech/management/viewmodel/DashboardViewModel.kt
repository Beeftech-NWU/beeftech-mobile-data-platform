package com.beeftech.management.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beeftech.management.data.DashboardSummary
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.ManagementResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    /* Kept across failed refreshes, so a dropped connection doesn't blank the screen. */
    val summary: DashboardSummary? = null,
    val loading: Boolean = false,
    val needsConnection: Boolean = false,
    val error: String? = null
)

class DashboardViewModel(
    private val apiClient: ManagementApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val result = apiClient.dashboardSummary()
            _uiState.update { state ->
                val done = state.copy(loading = false)
                when (result) {
                    is ManagementResult.Success ->
                        done.copy(summary = result.value, needsConnection = false, error = null)
                    is ManagementResult.NoConnection -> done.copy(needsConnection = true, error = null)
                    is ManagementResult.Unauthorized ->
                        done.copy(error = "Your session has expired. Log out and sign in again.")
                    is ManagementResult.Forbidden -> done.copy(error = result.message)
                    is ManagementResult.NotFound ->
                        done.copy(error = "The server doesn't support the dashboard yet. Update the server and try again.")
                    is ManagementResult.Rejected -> done.copy(error = result.message)
                    is ManagementResult.Error -> done.copy(error = result.message)
                }
            }
        }
    }
}

class DashboardViewModelFactory(
    private val apiClient: ManagementApiClient
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = DashboardViewModel(apiClient) as T
}
