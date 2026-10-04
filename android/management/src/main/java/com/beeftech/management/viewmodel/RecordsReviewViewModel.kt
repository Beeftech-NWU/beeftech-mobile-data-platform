package com.beeftech.management.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.ManagementResult
import com.beeftech.management.data.REVIEW_TYPES
import com.beeftech.management.data.ReviewRecord
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecordsReviewUiState(
    val type: String = REVIEW_TYPES.first().first,
    val records: List<ReviewRecord> = emptyList(),
    val showVoided: Boolean = true,
    val loading: Boolean = false,
    /* True when the last call failed for lack of connection, so the screen can say so. */
    val needsConnection: Boolean = false,
    val error: String? = null,
    val notice: String? = null
) {
    val visibleRecords: List<ReviewRecord>
        get() = if (showVoided) records else records.filterNot { it.isVoided }
}

class RecordsReviewViewModel(
    private val apiClient: ManagementApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordsReviewUiState())
    val uiState: StateFlow<RecordsReviewUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun selectType(type: String) {
        if (type == _uiState.value.type) return
        _uiState.update { it.copy(type = type, records = emptyList(), error = null, notice = null) }
        refresh()
    }

    fun setShowVoided(show: Boolean) {
        _uiState.update { it.copy(showVoided = show) }
    }

    fun refresh() = load(keepError = false)

    /*
     * keepError is for the reload that follows a rejected void: the list now shows the record as it
     * is, and the message explaining why the void was rejected must still be there when it arrives.
     */
    private fun load(keepError: Boolean) {
        /* A slower answer for a previous type must not overwrite the current one. */
        loadJob?.cancel()
        val type = _uiState.value.type
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val result = apiClient.reviewRecords(type)
            _uiState.update { state ->
                when (result) {
                    is ManagementResult.Success ->
                        state.copy(
                            records = result.value,
                            loading = false,
                            needsConnection = false,
                            error = if (keepError) state.error else null
                        )
                    else -> failed(state.copy(loading = false), result, LIST_NOT_FOUND)
                }
            }
        }
    }

    fun voidRecord(record: ReviewRecord, reason: String) {
        viewModelScope.launch {
            val result = apiClient.voidRecord(record.type, record.id, reason.trim())
            _uiState.update { state ->
                when (result) {
                    is ManagementResult.Success ->
                        state.copy(
                            records = state.records.map {
                                if (it.id == record.id && it.type == record.type) {
                                    it.copy(voidedAt = result.value.voidedAt, voidReason = reason.trim())
                                } else {
                                    it
                                }
                            },
                            notice = "Voided ${record.label}",
                            error = null,
                            needsConnection = false
                        )
                    is ManagementResult.Rejected -> failed(state, result)
                    else -> failed(state, result, RECORD_NOT_FOUND)
                }
            }
            /* Voided by someone else in the meantime: show it as it now is. Not inside update, which can run twice. */
            if (result is ManagementResult.Rejected) load(keepError = true)
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(notice = null, error = null) }
    }

    private fun failed(
        state: RecordsReviewUiState,
        result: ManagementResult<*>,
        notFoundMessage: String = RECORD_NOT_FOUND
    ): RecordsReviewUiState =
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
        const val RECORD_NOT_FOUND = "That record no longer exists."
        const val LIST_NOT_FOUND = "The server doesn't support records review yet. Update the server and try again."
    }
}

class RecordsReviewViewModelFactory(
    private val apiClient: ManagementApiClient
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = RecordsReviewViewModel(apiClient) as T
}
