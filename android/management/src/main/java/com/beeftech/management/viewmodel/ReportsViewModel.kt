package com.beeftech.management.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.ManagementResult
import com.beeftech.management.data.ReportData
import com.beeftech.management.data.ReportFile
import com.beeftech.management.data.ReportFormat
import com.beeftech.management.data.ReportKind
import com.beeftech.management.data.Site
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportsUiState(
    val kind: ReportKind = ReportKind.MORTALITY,
    val rangeDays: Int = 30,
    /* Only used by the calf registrations report: day, week or month. */
    val bucket: String = "week",
    val sites: List<Site> = emptyList(),
    val selectedSiteId: String? = null,
    /* Kept across failed refreshes, so a dropped connection doesn't blank the screen. */
    val report: ReportData? = null,
    val loading: Boolean = false,
    val exporting: Boolean = false,
    /* Set when an export is ready; the screen shares it and then calls [exportHandled]. */
    val exported: ReportFile? = null,
    val needsConnection: Boolean = false,
    val error: String? = null
)

val REPORT_RANGES = listOf(7, 30, 90)
val REPORT_BUCKETS = listOf("day", "week", "month")

/*
 * Managers always get their own site, so only an admin gets the site switch
 * ([canSwitchSite]); the server enforces the same rule.
 */
class ReportsViewModel(
    private val apiClient: ManagementApiClient,
    private val canSwitchSite: Boolean = false,
    private val now: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    private fun bucketFor(state: ReportsUiState) =
        if (state.kind == ReportKind.CALF_REGISTRATIONS) state.bucket else null

    private fun loadSites() {
        viewModelScope.launch {
            val result = apiClient.listSites()
            if (result is ManagementResult.Success) {
                _uiState.update { it.copy(sites = result.value) }
            }
        }
    }

    fun selectKind(kind: ReportKind) {
        if (kind == _uiState.value.kind) return
        /* The old report is for a different kind, so don't show it while the new one loads. */
        _uiState.update { it.copy(kind = kind, report = null) }
        refresh()
    }

    fun selectRange(days: Int) {
        if (days == _uiState.value.rangeDays) return
        _uiState.update { it.copy(rangeDays = days) }
        refresh()
    }

    fun selectBucket(bucket: String) {
        if (bucket == _uiState.value.bucket) return
        _uiState.update { it.copy(bucket = bucket) }
        if (_uiState.value.kind == ReportKind.CALF_REGISTRATIONS) refresh()
    }

    fun selectSite(siteId: String?) {
        if (!canSwitchSite || siteId == _uiState.value.selectedSiteId) return
        _uiState.update { it.copy(selectedSiteId = siteId) }
        refresh()
    }

    fun refresh() {
        if (canSwitchSite && _uiState.value.sites.isEmpty()) loadSites()
        /* A slower answer for an earlier choice must not overwrite the current one. */
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(loading = true) }
            val to = now()
            val result = apiClient.report(
                state.kind, to - state.rangeDays * DAY_MS, to, state.selectedSiteId, bucketFor(state)
            )
            _uiState.update { current ->
                val done = current.copy(loading = false)
                when (result) {
                    is ManagementResult.Success ->
                        done.copy(report = result.value, needsConnection = false, error = null)
                    else -> done.withFailure(result)
                }
            }
        }
    }

    fun export(format: ReportFormat) {
        val state = _uiState.value
        if (state.exporting) return
        _uiState.update { it.copy(exporting = true, error = null) }
        viewModelScope.launch {
            val to = now()
            val result = apiClient.reportFile(
                state.kind, format, to - state.rangeDays * DAY_MS, to, state.selectedSiteId, bucketFor(state)
            )
            _uiState.update { current ->
                val done = current.copy(exporting = false)
                when (result) {
                    is ManagementResult.Success -> done.copy(exported = result.value, needsConnection = false)
                    else -> done.withFailure(result)
                }
            }
        }
    }

    fun exportHandled() {
        _uiState.update { it.copy(exported = null) }
    }

    private fun ReportsUiState.withFailure(result: ManagementResult<*>): ReportsUiState =
        when (result) {
            is ManagementResult.NoConnection -> copy(needsConnection = true, error = null)
            is ManagementResult.Unauthorized ->
                copy(error = "Your session has expired. Log out and sign in again.")
            is ManagementResult.Forbidden -> copy(error = result.message)
            is ManagementResult.NotFound ->
                copy(error = "The server doesn't support reports yet. Update the server and try again.")
            is ManagementResult.Rejected -> copy(error = result.message)
            is ManagementResult.Error -> copy(error = result.message)
            is ManagementResult.Success -> this
        }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}

class ReportsViewModelFactory(
    private val apiClient: ManagementApiClient,
    private val canSwitchSite: Boolean = false
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ReportsViewModel(apiClient, canSwitchSite) as T
}
