package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beeftech.database.repository.SyncRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SyncStatusUiState(
    val pendingRecordCount: Int? = null,
    val lastSync: String = "",
    val syncStatus: String = "",
    val syncWarningLevel: Int = 0
)

class SyncStatusViewModel(
    repository: SyncRepository
) : ViewModel() {

    val uiState: StateFlow<SyncStatusUiState> =
        combine(
            repository.observePendingCount(),
            repository.observeLatestSyncBatch()
        ) { pendingCount, latestBatch ->

            SyncStatusUiState(
                pendingRecordCount = pendingCount,

                lastSync = latestBatch?.timestamp?.let {
                    formatTimestamp(it)
                } ?: "",

                syncStatus =
                    if (pendingCount == 0) {
                        "All records synced"
                    } else {
                        "$pendingCount record${if (pendingCount == 1) "" else "s"} waiting to sync"
                    },

                syncWarningLevel =
                    if (pendingCount > 0) {
                        1
                    } else {
                        0
                    }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SyncStatusUiState()
        )

    private fun formatTimestamp(
        timestamp: Long
    ): String {
        return SimpleDateFormat(
            "dd MMM yyyy, HH:mm",
            Locale.getDefault()
        ).format(
            Date(timestamp)
        )
    }
}

class SyncStatusViewModelFactory(
    private val repository: SyncRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                SyncStatusViewModel::class.java
            )
        ) {
            return SyncStatusViewModel(
                repository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}