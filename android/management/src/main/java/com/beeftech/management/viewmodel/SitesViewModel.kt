package com.beeftech.management.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.ManagementResult
import com.beeftech.management.data.Site
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SitesUiState(
    val sites: List<Site> = emptyList(),
    val loading: Boolean = false,
    /* True when the last call failed for lack of connection, so the screen can say so. */
    val needsConnection: Boolean = false,
    val error: String? = null,
    val notice: String? = null
)

fun normaliseFarmCode(input: String): String = input.trim().uppercase()

/* Four characters, A-Z and 0-9, as the server requires. */
fun isValidFarmCode(input: String): Boolean = Regex("^[A-Z0-9]{4}$").matches(normaliseFarmCode(input))

class SitesViewModel(
    private val apiClient: ManagementApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SitesUiState())
    val uiState: StateFlow<SitesUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val result = apiClient.listSites()
            _uiState.update { state ->
                when (result) {
                    is ManagementResult.Success ->
                        state.copy(sites = result.value, loading = false, needsConnection = false, error = null)
                    else -> failed(state.copy(loading = false), result, LIST_NOT_FOUND)
                }
            }
        }
    }

    fun createSite(name: String, farmCode: String, onCreated: () -> Unit = {}) {
        viewModelScope.launch {
            val result = apiClient.createSite(name.trim(), normaliseFarmCode(farmCode))
            if (result is ManagementResult.Success) {
                _uiState.update {
                    it.copy(
                        sites = (it.sites + result.value).sortedBy { s -> s.name.lowercase() },
                        notice = "Created ${result.value.name}",
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

    fun rename(site: Site, name: String, onRenamed: () -> Unit = {}) {
        viewModelScope.launch {
            val result = apiClient.updateSite(site.siteId, name = name.trim())
            if (applySiteResult(result, "Renamed to ${name.trim()}")) onRenamed()
        }
    }

    fun changeFarmCode(site: Site, farmCode: String, onChanged: () -> Unit = {}) {
        viewModelScope.launch {
            val code = normaliseFarmCode(farmCode)
            val result = apiClient.updateSite(site.siteId, farmCode = code)
            if (applySiteResult(result, "Farm code for ${site.name} is now $code")) onChanged()
        }
    }

    fun setActive(site: Site, active: Boolean) {
        viewModelScope.launch {
            val result = apiClient.updateSite(site.siteId, active = active)
            applySiteResult(result, if (active) "Reactivated ${site.name}" else "Deactivated ${site.name}")
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(notice = null, error = null) }
    }

    /* Returns true on success. */
    private fun applySiteResult(result: ManagementResult<Site>, notice: String): Boolean {
        _uiState.update { state ->
            if (result is ManagementResult.Success) {
                state.copy(
                    sites = state.sites
                        .map { if (it.siteId == result.value.siteId) result.value else it }
                        .sortedBy { it.name.lowercase() },
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

    /*
     * A 404 on a single site means it was removed; on the list it means the server has no
     * sites endpoint (e.g. an older backend). A 409 (duplicate name, or a site that still has
     * active users) arrives as Rejected with the server's own message.
     */
    private fun failed(
        state: SitesUiState,
        result: ManagementResult<*>,
        notFoundMessage: String = SITE_NOT_FOUND
    ): SitesUiState =
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
        const val SITE_NOT_FOUND = "That site no longer exists."
        const val LIST_NOT_FOUND = "The server doesn't support sites yet. Update the server and try again."
    }
}

class SitesViewModelFactory(
    private val apiClient: ManagementApiClient
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SitesViewModel(apiClient) as T
}
