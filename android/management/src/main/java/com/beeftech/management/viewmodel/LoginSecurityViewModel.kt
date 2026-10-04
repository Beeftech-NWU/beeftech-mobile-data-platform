package com.beeftech.management.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beeftech.management.data.Lockout
import com.beeftech.management.data.LoginEvent
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.ManagementResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginSecurityUiState(
    val lockouts: List<Lockout> = emptyList(),
    val events: List<LoginEvent> = emptyList(),
    /* Null means every outcome. */
    val outcome: String? = null,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    /* True while the last page came back full, so there may be older events. */
    val canLoadMore: Boolean = false,
    /* True when the last call failed for lack of connection, so the screen can say so. */
    val needsConnection: Boolean = false,
    val error: String? = null,
    val notice: String? = null
)

/* Admin only: who is locked out now, and every sign-in attempt. */
class LoginSecurityViewModel(
    private val apiClient: ManagementApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginSecurityUiState())
    val uiState: StateFlow<LoginSecurityUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun selectOutcome(outcome: String?) {
        if (outcome == _uiState.value.outcome) return
        _uiState.update { it.copy(outcome = outcome) }
        refresh()
    }

    fun refresh() {
        /* A slower answer for an earlier filter must not overwrite the current one. */
        loadJob?.cancel()
        val outcome = _uiState.value.outcome
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(loading = true, loadingMore = false, events = emptyList(), canLoadMore = false) }
            val lockouts = apiClient.lockouts()
            val events = apiClient.loginEvents(outcome = outcome)
            _uiState.update { state ->
                val bad = listOf(lockouts, events).firstOrNull { it !is ManagementResult.Success }
                if (bad != null) {
                    failed(state.copy(loading = false), bad)
                } else {
                    val page = (events as ManagementResult.Success).value
                    state.copy(
                        lockouts = (lockouts as ManagementResult.Success).value,
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
            val result = apiClient.loginEvents(outcome = state.outcome, before = last.id)
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

    /* Lifts the lock without changing the PIN. A name that isn't a real user has no id and can't be unlocked. */
    fun unlock(lockout: Lockout) {
        val userId = lockout.userId ?: return
        viewModelScope.launch {
            val result = apiClient.unlockLogin(userId)
            _uiState.update { state ->
                if (result is ManagementResult.Success) {
                    state.copy(
                        lockouts = state.lockouts.filterNot { it.username == lockout.username },
                        notice = "Unlocked ${lockout.username}",
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

    private fun failed(state: LoginSecurityUiState, result: ManagementResult<*>): LoginSecurityUiState =
        when (result) {
            is ManagementResult.NoConnection -> state.copy(needsConnection = true, error = null)
            is ManagementResult.Unauthorized ->
                state.copy(error = "Your session has expired. Log out and sign in again.")
            is ManagementResult.Forbidden -> state.copy(error = result.message)
            is ManagementResult.NotFound ->
                state.copy(error = "The server doesn't support login security yet. Update the server and try again.")
            is ManagementResult.Rejected -> state.copy(error = result.message)
            is ManagementResult.Error -> state.copy(error = result.message)
            is ManagementResult.Success -> state
        }
}

class LoginSecurityViewModelFactory(
    private val apiClient: ManagementApiClient
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = LoginSecurityViewModel(apiClient) as T
}
