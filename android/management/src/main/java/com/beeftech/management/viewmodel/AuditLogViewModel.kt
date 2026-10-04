package com.beeftech.management.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beeftech.management.data.AuditLogEntry
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.ManagementResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuditRange(val label: String, val millis: Long?) {
    LAST_DAY("24 hours", 24L * 60 * 60 * 1000),
    LAST_WEEK("7 days", 7L * 24 * 60 * 60 * 1000),
    LAST_MONTH("30 days", 30L * 24 * 60 * 60 * 1000),
    ALL("All", null)
}

data class AuditLogUiState(
    val entries: List<AuditLogEntry> = emptyList(),
    /* Null means every action. */
    val action: String? = null,
    val range: AuditRange = AuditRange.LAST_WEEK,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    /* True while the last page came back full, so there may be older entries. */
    val canLoadMore: Boolean = false,
    /* True when the last call failed for lack of connection, so the screen can say so. */
    val needsConnection: Boolean = false,
    val error: String? = null
)

class AuditLogViewModel(
    private val apiClient: ManagementApiClient,
    private val now: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuditLogUiState())
    val uiState: StateFlow<AuditLogUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun selectAction(action: String?) {
        if (action == _uiState.value.action) return
        _uiState.update { it.copy(action = action) }
        refresh()
    }

    fun selectRange(range: AuditRange) {
        if (range == _uiState.value.range) return
        _uiState.update { it.copy(range = range) }
        refresh()
    }

    fun refresh() {
        /* A slower answer for an earlier filter must not overwrite the current one. */
        loadJob?.cancel()
        val state = _uiState.value
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(loading = true, loadingMore = false, entries = emptyList(), canLoadMore = false) }
            val result = apiClient.auditLog(action = state.action, from = fromTime(state.range))
            _uiState.update { current ->
                when (result) {
                    is ManagementResult.Success -> current.copy(
                        entries = result.value,
                        loading = false,
                        canLoadMore = result.value.size >= ManagementApiClient.AUDIT_PAGE_SIZE,
                        needsConnection = false,
                        error = null
                    )
                    else -> failed(current.copy(loading = false), result)
                }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        val last = state.entries.lastOrNull()
        if (last == null || !state.canLoadMore || state.loading || state.loadingMore) return

        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            val result = apiClient.auditLog(
                action = state.action,
                from = fromTime(state.range),
                before = last.id
            )
            _uiState.update { current ->
                when (result) {
                    is ManagementResult.Success -> current.copy(
                        entries = current.entries + result.value,
                        loadingMore = false,
                        canLoadMore = result.value.size >= ManagementApiClient.AUDIT_PAGE_SIZE,
                        needsConnection = false,
                        error = null
                    )
                    else -> failed(current.copy(loadingMore = false), result)
                }
            }
        }
    }

    private fun fromTime(range: AuditRange): Long? = range.millis?.let { now() - it }

    private fun failed(state: AuditLogUiState, result: ManagementResult<*>): AuditLogUiState =
        when (result) {
            is ManagementResult.NoConnection -> state.copy(needsConnection = true, error = null)
            is ManagementResult.Unauthorized ->
                state.copy(error = "Your session has expired. Log out and sign in again.")
            is ManagementResult.Forbidden -> state.copy(error = result.message)
            is ManagementResult.NotFound ->
                state.copy(error = "The server doesn't support the audit log yet. Update the server and try again.")
            is ManagementResult.Rejected -> state.copy(error = result.message)
            is ManagementResult.Error -> state.copy(error = result.message)
            is ManagementResult.Success -> state
        }
}

class AuditLogViewModelFactory(
    private val apiClient: ManagementApiClient
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AuditLogViewModel(apiClient) as T
}
