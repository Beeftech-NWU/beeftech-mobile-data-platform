package com.beeftech.management.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beeftech.management.data.CreateReferenceBody
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.ManagementResult
import com.beeftech.management.data.ReferenceEntry
import com.beeftech.management.data.ReferenceSnapshotDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReferenceDataUiState(
    /* One of REFERENCE_KINDS' slugs. */
    val kind: String = "diseases",
    val entries: List<ReferenceEntry> = emptyList(),
    val version: Long? = null,
    val loading: Boolean = false,
    /* True when the last call failed for lack of connection, so the screen can say so. */
    val needsConnection: Boolean = false,
    val error: String? = null,
    val notice: String? = null
) {
    val visibleEntries: List<ReferenceEntry> get() = entries.filter { it.kind == kind }
}

/*
 * Admin screen for the values the app's pickers offer. Values can be added and turned on or off;
 * there is no rename or delete, because the app's records point at a value by its name or code.
 */
class ReferenceDataViewModel(
    private val apiClient: ManagementApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReferenceDataUiState())
    val uiState: StateFlow<ReferenceDataUiState> = _uiState.asStateFlow()

    fun selectKind(kind: String) {
        _uiState.update { it.copy(kind = kind, notice = null) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val result = apiClient.referenceData()
            _uiState.update { state ->
                when (result) {
                    is ManagementResult.Success ->
                        state.copy(
                            entries = result.value.toEntries(),
                            version = result.value.version,
                            loading = false,
                            needsConnection = false,
                            error = null
                        )
                    else -> failed(state.copy(loading = false), result, LIST_NOT_FOUND)
                }
            }
        }
    }

    /* A disease or treatment type needs only a name; a cost type needs a code and a name. */
    fun add(name: String, code: String? = null, onAdded: () -> Unit = {}) {
        val kind = _uiState.value.kind
        viewModelScope.launch {
            val body = if (kind == COST_TYPES) {
                CreateReferenceBody(code = code?.trim(), displayName = name.trim())
            } else {
                CreateReferenceBody(name = name.trim())
            }
            val result = apiClient.createReferenceValue(kind, body)
            if (result is ManagementResult.Success) {
                _uiState.update {
                    it.copy(
                        entries = (it.entries + result.value.item).sortedWith(ENTRY_ORDER),
                        version = result.value.version,
                        notice = "Added ${result.value.item.name}",
                        error = null,
                        needsConnection = false
                    )
                }
                onAdded()
            } else {
                _uiState.update { failed(it, result) }
            }
        }
    }

    fun setActive(entry: ReferenceEntry, active: Boolean) {
        viewModelScope.launch {
            val result = apiClient.setReferenceActive(entry.kind, entry.id, active)
            _uiState.update { state ->
                if (result is ManagementResult.Success) {
                    state.copy(
                        entries = state.entries.map {
                            if (it.kind == entry.kind && it.id == entry.id) result.value.item else it
                        },
                        version = result.value.version,
                        notice = if (active) "Turned on ${entry.name}" else "Turned off ${entry.name}",
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

    /*
     * A 404 on one value means it no longer exists; on the list it means the server has no
     * reference-data endpoint (e.g. an older backend). A 409 (duplicate name, or the protected
     * Treatment cost type) arrives as Rejected with the server's own message.
     */
    private fun failed(
        state: ReferenceDataUiState,
        result: ManagementResult<*>,
        notFoundMessage: String = VALUE_NOT_FOUND
    ): ReferenceDataUiState =
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
        const val COST_TYPES = "cost-types"
        const val VALUE_NOT_FOUND = "That value no longer exists."
        const val LIST_NOT_FOUND = "The server doesn't support reference data yet. Update the server and try again."

        /* Names in order within a kind; cost types keep the server's order. */
        val ENTRY_ORDER = compareBy<ReferenceEntry>({ it.kind }, { it.sortOrder ?: 0 }, { it.name.lowercase() })
    }
}

internal fun ReferenceSnapshotDto.toEntries(): List<ReferenceEntry> =
    diseases.orEmpty().map { ReferenceEntry("diseases", it.id.toString(), it.name, it.active) } +
        treatmentTypes.orEmpty().map { ReferenceEntry("treatment-types", it.id.toString(), it.name, it.active) } +
        costTypes.orEmpty().map { ReferenceEntry("cost-types", it.code, it.displayName, it.active, it.sortOrder) }

class ReferenceDataViewModelFactory(
    private val apiClient: ManagementApiClient
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ReferenceDataViewModel(apiClient) as T
}
