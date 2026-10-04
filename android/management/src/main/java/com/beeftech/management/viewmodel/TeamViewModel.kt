package com.beeftech.management.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beeftech.management.data.CreateUserBody
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.ManagementResult
import com.beeftech.management.data.Site
import com.beeftech.management.data.TeamMember
import com.beeftech.management.data.UpdateUserBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IssuedPin(
    val username: String,
    val pin: String
)

data class TeamUiState(
    val members: List<TeamMember> = emptyList(),
    /* Loaded for admins only, so the Add user dialog can offer a site picker. */
    val sites: List<Site> = emptyList(),
    val loading: Boolean = false,
    /* True when the last call failed for lack of connection, so the screen can say so. */
    val needsConnection: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    /* Shown once so the manager can hand it to the worker. */
    val issuedPin: IssuedPin? = null
)

class TeamViewModel(
    private val apiClient: ManagementApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeamUiState())
    val uiState: StateFlow<TeamUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val result = apiClient.listUsers()
            _uiState.update { state ->
                when (result) {
                    is ManagementResult.Success ->
                        state.copy(
                            members = result.value,
                            loading = false,
                            needsConnection = false,
                            error = null
                        )
                    else -> failed(state.copy(loading = false), result, LIST_NOT_FOUND)
                }
            }
        }
    }

    /* A failure here leaves the old list; the dialog just offers what it has. */
    fun loadSites() {
        viewModelScope.launch {
            val result = apiClient.listSites()
            if (result is ManagementResult.Success) {
                _uiState.update { it.copy(sites = result.value) }
            }
        }
    }

    fun createWorker(
        username: String,
        pin: String,
        siteId: String? = null,
        onCreated: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = apiClient.createUser(
                CreateUserBody(username = username.trim(), pin = pin, siteId = siteId?.trim()?.ifBlank { null })
            )
            if (result is ManagementResult.Success) {
                _uiState.update {
                    it.copy(
                        members = (it.members + result.value).sortedBy { m -> m.username },
                        notice = "Created ${result.value.username}",
                        error = null,
                        needsConnection = false
                    )
                }
                onCreated()
            } else {
                _uiState.update { failed(it, result) }
            }
        }
    }

    fun setActive(member: TeamMember, active: Boolean) {
        viewModelScope.launch {
            val result = apiClient.updateUser(member.userId, UpdateUserBody(active = active))
            applyMemberResult(
                result,
                if (active) "Reactivated ${member.username}" else "Deactivated ${member.username}"
            )
        }
    }

    fun unbindDevice(member: TeamMember) {
        viewModelScope.launch {
            val result = apiClient.unbindDevice(member.userId)
            applyMemberResult(result, "${member.username} can now sign in from a new phone")
        }
    }

    /* For a worker locked out after five wrong PINs; the PIN itself is unchanged. */
    fun unlockLogin(member: TeamMember) {
        viewModelScope.launch {
            val result = apiClient.unlockLogin(member.userId)
            applyMemberResult(result, "${member.username} can sign in again")
        }
    }

    fun resetPin(member: TeamMember) {
        viewModelScope.launch {
            val result = apiClient.resetPin(member.userId)
            _uiState.update { state ->
                if (result is ManagementResult.Success) {
                    state.copy(
                        issuedPin = IssuedPin(member.username, result.value.pin),
                        error = null,
                        needsConnection = false
                    )
                } else {
                    failed(state, result)
                }
            }
        }
    }

    fun dismissIssuedPin() {
        _uiState.update { it.copy(issuedPin = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(notice = null, error = null) }
    }

    private fun applyMemberResult(result: ManagementResult<TeamMember>, notice: String) {
        _uiState.update { state ->
            if (result is ManagementResult.Success) {
                state.copy(
                    members = state.members.map { if (it.userId == result.value.userId) result.value else it },
                    notice = notice,
                    error = null,
                    needsConnection = false
                )
            } else {
                failed(state, result)
            }
        }
    }

    /*
     * A 404 on a single user means they were removed or moved out of scope; on the list it
     * means the server has no team endpoint (e.g. an older backend), so say that instead.
     */
    private fun failed(
        state: TeamUiState,
        result: ManagementResult<*>,
        notFoundMessage: String = USER_NOT_FOUND
    ): TeamUiState =
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
        const val USER_NOT_FOUND = "That user no longer exists."
        const val LIST_NOT_FOUND = "The server doesn't support team management yet. Update the server and try again."
    }
}


class TeamViewModelFactory(
    private val apiClient: ManagementApiClient
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = TeamViewModel(apiClient) as T
}
