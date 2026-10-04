package com.beeftech.management.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beeftech.management.data.LockedAccount
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.ManagementResult
import com.beeftech.management.data.SecurityEventRow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SyncSecurityUiState(
    val locked: List<LockedAccount> = emptyList(),
    val events: List<SecurityEventRow> = emptyList(),
    /* Null means every event type. */
    val eventType: String? = null,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    /* True while the last page came back full, so there may be older events. */
    val canLoadMore: Boolean = false,
    /* True when the last call failed for lack of connection, so the screen can say so. */
    val needsConnection: Boolean = false,
    val error: String? = null,
    val notice: String? = null
)

/* Admin only: accounts locked by the 7-day rule (with Clear lock), and what the phones reported. */
class SyncSecurityViewModel(
    private val apiClient: ManagementApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncSecurityUiState())
    val uiState: StateFlow<SyncSecurityUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun selectEventType(eventType: String?) {
        if (eventType == _uiState.value.eventType) return
        _uiState.update { it.copy(eventType = eventType) }
        refresh()
    }

    fun refresh() {
        /* A slower answer for an earlier filter must not overwrite the current one. */
        loadJob?.cancel()
        val eventType = _uiState.value.eventType
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(loading = true, loadingMore = false, events = emptyList(), canLoadMore = false) }
            val locked = apiClient.lockedAccounts()
            val events = apiClient.securityEvents(eventType = eventType)
            _uiState.update { state ->
                val bad = listOf(locked, events).firstOrNull { it !is ManagementResult.Success }
                if (bad != null) {
                    failed(state.copy(loading = false), bad)
                } else {
                    val page = (events as ManagementResult.Success).value
                    state.copy(
                        locked = (locked as ManagementResult.Success).value,
                        events = page,
                        loading = false,
                        canLoadMore = page.size >= ManagementApiClient.AUDIT_PAGE_SIZE,
                        needsConnection = false,
                        error = null
                    )
                }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        val last = state.events.lastOrNull()
        if (last == null || !state.canLoadMore || state.loading || state.loadingMore) return

        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            val result = apiClient.securityEvents(eventType = state.eventType, before = last.id)
            _uiState.update { current ->
                when (result) {
                    is ManagementResult.Success -> current.copy(
                        events = current.events + result.value,
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

    /* The user's phone lifts the lock the next time it checks in. Wiped data does not come back. */
    fun clearLock(account: LockedAccount) {
        viewModelScope.launch {
            val result = apiClient.clearSyncLock(account.userId)
            _uiState.update { state ->
                if (result is ManagementResult.Success) {
                    state.copy(
                        locked = state.locked.filterNot { it.userId == account.userId },
                        notice = "Cleared the lock for ${account.username}. Their phone lifts it the next time it connects.",
                        error = null,
                        needsConnection = false
                    )
                } else {
                    failed(state, result)
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(notice = null, error = null) }
    }

    private fun failed(state: SyncSecurityUiState, result: ManagementResult<*>): SyncSecurityUiState =
        when (result) {
            is ManagementResult.NoConnection -> state.copy(needsConnection = true, error = null)
            is ManagementResult.Unauthorized ->
                state.copy(error = "Your session has expired. Log out and sign in again.")
            is ManagementResult.Forbidden -> state.copy(error = result.message)
            is ManagementResult.NotFound ->
                state.copy(error = "The server doesn't support sync security yet. Update the server and try again.")
            is ManagementResult.Rejected -> state.copy(error = result.message)
            is ManagementResult.Error -> state.copy(error = result.message)
            is ManagementResult.Success -> state
        }
}

class SyncSecurityViewModelFactory(
    private val apiClient: ManagementApiClient
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SyncSecurityViewModel(apiClient) as T
}
