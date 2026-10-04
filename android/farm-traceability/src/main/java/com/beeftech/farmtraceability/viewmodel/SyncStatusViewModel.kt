package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beeftech.database.repository.SyncRepository
import com.beeftech.database.repository.SyncWarningPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SyncStatusUiState(
    val pendingRecordCount: Int? = null,
    val lastSync: String = "",
    val syncStatus: String = "",
    val syncWarningLevel: Int = 0,
    val oldestPendingAgeDays: Long = 0
)

class SyncStatusViewModel(
    repository: SyncRepository,
    /* Where the warning days come from (the server can change them); the default if not given. */
    private val policyProvider: suspend () -> SyncWarningPolicy = { SyncWarningPolicy.DEFAULT }
) : ViewModel() {

    /*
     * Re-check time once per hour so warning levels can increase
     * even when no pending-sync database row changes.
     */
    private val clock: Flow<Long> =
        flow {
            while (true) {
                emit(System.currentTimeMillis())

                delay(
                    ONE_HOUR_MS
                )
            }
        }

    val uiState: StateFlow<SyncStatusUiState> =
        combine(
            repository.observePendingCount(),
            repository.observeLatestSyncBatch(),
            repository.observeOldestPendingCreatedAt(),
            clock
        ) {
                pendingCount,
                latestBatch,
                oldestPendingCreatedAt,
                currentTime ->

            val ageDays =
                calculatePendingAgeDays(
                    pendingCount =
                        pendingCount,

                    oldestPendingCreatedAt =
                        oldestPendingCreatedAt,

                    currentTime =
                        currentTime
                )

            SyncStatusUiState(
                pendingRecordCount =
                    pendingCount,

                lastSync =
                    latestBatch
                        ?.timestamp
                        ?.let {
                            formatTimestamp(it)
                        }
                        ?: "",

                syncStatus =
                    if (pendingCount == 0) {
                        "All records synced"
                    } else {
                        "$pendingCount record${if (pendingCount == 1) "" else "s"} waiting to sync"
                    },

                syncWarningLevel =
                    calculateWarningLevel(
                        pendingCount =
                            pendingCount,

                        ageDays =
                            ageDays,

                        policy =
                            loadPolicy()
                    ),

                oldestPendingAgeDays =
                    ageDays
            )
        }.stateIn(
            scope =
                viewModelScope,

            started =
                SharingStarted
                    .WhileSubscribed(5000),

            initialValue =
                SyncStatusUiState()
        )

    /* A policy that can't be read is the default; the status card must never fail over it. */
    private suspend fun loadPolicy(): SyncWarningPolicy =
        try {
            policyProvider()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SyncWarningPolicy.DEFAULT
        }

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

    companion object {

        private const val ONE_HOUR_MS =
            60L * 60L * 1000L

        private const val ONE_DAY_MS =
            24L * 60L * 60L * 1000L

        internal fun calculatePendingAgeDays(
            pendingCount: Int,
            oldestPendingCreatedAt: Long?,
            currentTime: Long
        ): Long {

            if (
                pendingCount <= 0 ||
                oldestPendingCreatedAt == null
            ) {
                return 0
            }

            val ageMillis =
                (
                    currentTime -
                            oldestPendingCreatedAt
                    )
                    .coerceAtLeast(0L)

            return ageMillis /
                    ONE_DAY_MS
        }

        /* The same levels the enforcer uses, so the card and the lock screen never disagree. */
        internal fun calculateWarningLevel(
            pendingCount: Int,
            ageDays: Long,
            policy: SyncWarningPolicy = SyncWarningPolicy.DEFAULT
        ): Int {

            if (pendingCount <= 0) {
                return 0
            }

            return policy.levelFor(ageDays)
        }
    }
}

class SyncStatusViewModelFactory(
    private val repository: SyncRepository,
    private val policyProvider: suspend () -> SyncWarningPolicy = { SyncWarningPolicy.DEFAULT }
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
                repository,
                policyProvider
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}
