package com.beeftech.feedcrib.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.beeftech.database.dao.FeedCribSessionRow
import com.beeftech.database.entity.CribReadingCodeEntity
import com.beeftech.database.entity.FeedCribEntity
import com.beeftech.database.entity.FeedCribEntryEntity
import com.beeftech.feedcrib.data.FeedCribRepository
import com.beeftech.feedcrib.data.SlotAllocator
import com.beeftech.feedcrib.worker.FeedCribSyncWorker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** What the worker has chosen for the open crib and not saved yet. */
data class CribDraft(
    val code: Int? = null,
    val adi: Double = 0.0,
    val adiChanged: Boolean = false
) {
    /** Discard asks "are you sure?" only when this is true. */
    val changed: Boolean get() = code != null || adiChanged
}

/** The open crib: its details, the grid, the draft and the block a reading would go into right now. */
data class CribDetailState(
    val crib: FeedCribEntity,
    val draft: CribDraft,
    /** The shown code of each date and block over the last 3 days, newest date first. */
    val lastSlots: List<FeedCribEntryEntity>,
    /** Where a reading saved now would be filed: MORNING, MIDDAY or EVENING. */
    val currentSlot: String,
    /** The date a reading saved now would carry, yyyy-MM-dd. */
    val currentDate: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class FeedCribViewModel(
    private val repository: FeedCribRepository,
    private val applicationContext: Context,
    private val clock: () -> Long = System::currentTimeMillis,
    private val timeZone: TimeZone = TimeZone.getDefault()
) : ViewModel() {

    val cribs: StateFlow<List<FeedCribEntity>> =
        repository.observeCribs().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val codes: StateFlow<List<CribReadingCodeEntity>> =
        repository.observeCodes().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** When the cribs were last downloaded, or null if never. */
    val lastDownloadedAt: StateFlow<Long?> =
        repository.observeLastDownloadedAt().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Bumped to re-read "today" when the sessions screen opens, so a phone left on overnight rolls over. */
    private val sessionsTick = MutableStateFlow(0)

    val sessions: StateFlow<List<FeedCribSessionRow>> =
        sessionsTick.flatMapLatest { repository.observeTodaySessions() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val selectedCribNumber = MutableStateFlow<String?>(null)
    private val draft = MutableStateFlow(CribDraft())

    /** The open crib, or null when none is open. */
    val detail: StateFlow<CribDetailState?> =
        combine(
            selectedCribNumber.flatMapLatest { number ->
                if (number == null) {
                    flowOf(null)
                } else {
                    repository.observeLastNineSlots(number).map { number to it }
                }
            },
            cribs,
            draft
        ) { slots, allCribs, currentDraft ->
            val crib = slots?.let { (number, _) -> allCribs.firstOrNull { it.cribNumber == number } }
            if (slots == null || crib == null) {
                null
            } else {
                val now = clock()
                CribDetailState(
                    crib = crib,
                    draft = currentDraft,
                    lastSlots = slots.second,
                    currentSlot = SlotAllocator.slotFor(now, timeZone),
                    currentDate = SlotAllocator.dateFor(now, timeZone)
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        // Backup periodic sync. This is not the primary sync mechanism.
        schedulePeriodicSync()

        // Also queue an immediate network-constrained check. If the device
        // is offline, WorkManager keeps it waiting until connectivity returns.
        scheduleNetworkAvailableSync()
    }

    // --- opening a crib ---

    /**
     * Opens a crib by the number the worker typed or picked (case does not matter). The draft
     * starts at the crib's current ADI with no code chosen. Answers false if the phone has no
     * such crib.
     */
    fun openCrib(cribNumber: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val crib = repository.findCrib(cribNumber)
            if (crib == null) {
                onResult(false)
                return@launch
            }
            draft.value = CribDraft(code = null, adi = crib.currentAdi ?: 0.0)
            selectedCribNumber.value = crib.cribNumber
            onResult(true)
        }
    }

    /** Picks a code; picking the chosen one again clears it. */
    fun selectCode(code: Int?) {
        draft.value = draft.value.let { it.copy(code = if (it.code == code) null else code) }
    }

    /** One key press is 0.1 kg; [steps] may be negative. ADI never goes below zero. */
    fun adjustAdi(steps: Int) {
        val current = draft.value
        draft.value = current.copy(adi = SlotAllocator.adjustAdi(current.adi, steps), adiChanged = true)
    }

    /** Closes the crib without saving. The screen asks first when [CribDraft.changed]. */
    fun discard() {
        selectedCribNumber.value = null
        draft.value = CribDraft()
    }

    /**
     * Saves the draft as a new reading and closes the crib, ready for the next one. The message is
     * for the snackbar, e.g. "Saved A06 · Mid-Day 3". A reading the server did not take yet is
     * still saved, and says it will send when online.
     */
    fun save(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        /*
         * Read the open crib and the draft themselves, not the derived `detail` flow, which can
         * lag a tap by a frame. What the worker last pressed is what gets saved.
         */
        val cribNumber = selectedCribNumber.value
        val currentDraft = draft.value

        if (cribNumber == null) {
            onResult(false, "Open a crib first.")
            return
        }

        if (!currentDraft.changed) {
            onResult(false, "Pick a reading code or change the ADI.")
            return
        }

        viewModelScope.launch {
            val outcome = repository.saveEntry(cribNumber, currentDraft.code, currentDraft.adi)

            (outcome.validationError ?: outcome.saveError)?.let {
                onResult(false, it)
                return@launch
            }

            val saved = outcome.entry!!
            val what = saved.code?.let { "${SlotAllocator.label(saved.slot)} $it" } ?: "ADI ${saved.adi}"

            val message = when {
                outcome.needsAttention -> "Saved ${saved.cribNumber} · $what · the server refused it"
                outcome.syncErrorMessage != null -> "Saved ${saved.cribNumber} · $what · will send when online"
                else -> "Saved ${saved.cribNumber} · $what"
            }

            discard()
            if (outcome.syncErrorMessage != null) scheduleNetworkAvailableSync()
            onResult(true, message)
        }
    }

    // --- sessions ---

    /** Re-reads "today" for the session list. */
    fun loadSessions() {
        sessionsTick.value += 1
    }

    /** Every entry the user took on a crib today, for the read-only detail. */
    fun entriesToday(cribNumber: String): Flow<List<FeedCribEntryEntity>> =
        repository.observeTodayEntries(cribNumber)

    // --- download and retry ---

    /** Downloads the site's cribs, codes and recent readings. */
    fun refresh(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _refreshing.value = true
            try {
                repository.refreshCribs().fold(
                    onSuccess = { count -> onResult(true, if (count == 1) "1 crib loaded." else "$count cribs loaded.") },
                    onFailure = { onResult(false, it.message ?: "Could not load the cribs.") }
                )
            } finally {
                _refreshing.value = false
            }
        }
    }

    /** Sends everything waiting now (the Sync button). Rejected readings get a fresh set of attempts. */
    fun retrySync(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val outcome = repository.syncPending(retryRejected = true)

                when {
                    outcome.errorMessagesByRecordGuid.isNotEmpty() -> {
                        scheduleNetworkAvailableSync()
                        onResult(false, outcome.errorMessagesByRecordGuid.values.firstNotNullOfOrNull { it }
                            ?: "Some readings are still waiting to send.")
                    }
                    outcome.rejectedByRecordGuid.isNotEmpty() ->
                        onResult(false, "The server refused a reading: ${outcome.rejectedByRecordGuid.values.first()}")
                    outcome.syncedCount > 0 ->
                        onResult(true, if (outcome.syncedCount == 1) "1 reading sent." else "${outcome.syncedCount} readings sent.")
                    else ->
                        onResult(true, "There are no readings waiting to send.")
                }
            } catch (exception: Exception) {
                scheduleNetworkAvailableSync()
                onResult(false, exception.message ?: "Unable to retry the feed crib sync.")
            }
        }
    }

    /**
     * Primary background retry: a one-time request constrained by CONNECTED network state. When
     * created while offline, WorkManager waits until the constraint is met instead of waiting for
     * the 15-minute periodic job.
     */
    private fun scheduleNetworkAvailableSync() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<FeedCribSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                NETWORK_AVAILABLE_SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        } catch (_: Exception) {
            // Unit tests or uninitialized WorkManager runtime
        }
    }

    /** Safety-net periodic sync. WorkManager periodic execution is inexact, so it is not relied on for Save. */
    private fun schedulePeriodicSync() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<FeedCribSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        } catch (_: Exception) {
            // Unit tests or uninitialized WorkManager runtime
        }
    }

    companion object {
        const val NETWORK_AVAILABLE_SYNC_WORK_NAME = "feed-crib-network-available-sync"
        const val PERIODIC_SYNC_WORK_NAME = "feed-crib-periodic-sync"
    }
}
