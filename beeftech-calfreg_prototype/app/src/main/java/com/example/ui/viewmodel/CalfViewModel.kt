package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CalfRegistration
import com.example.data.model.SyncLog
import com.example.data.repository.CalfRepository
import com.example.data.repository.RegistrationResult
import com.example.data.repository.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class RegistrationFormState(
    val animalId: String = "",
    val birthDate: String = "",
    val breed: String = "Bonsmara",
    val sex: String = CalfRegistration.SEX_HEIFER,
    val birthWeightKg: Double? = 34.0,
    val calvingEase: Int = CalfRegistration.CALVING_EASE_UNASSISTED,
    val vigor: String = CalfRegistration.VIGOR_VIGOROUS,
    val hornStatus: String = CalfRegistration.HORN_POLLED,
    val pastureLocation: String = "Camp 4 - North Pasture",
    val rfidTag: String = "",
    val damId: String = "",
    val sireId: String = "",
    val photoPath: String? = null,
    val videoPath: String? = null,
    val isCheckingDuplicate: Boolean = false,
    val isDuplicateId: Boolean = false,
    val duplicateErrorMessage: String? = null,
    val isSubmitting: Boolean = false,
    val submissionSuccess: CalfRegistration? = null,
    val errorMessage: String? = null,
    val generatedGuidPreview: String = UUID.randomUUID().toString(),
    val mockGpsLat: Double = -25.7461,
    val mockGpsLng: Double = 28.1881
) {
    val isFormValid: Boolean
        get() = animalId.isNotBlank() &&
                birthDate.isNotBlank() &&
                breed.isNotBlank() &&
                !isDuplicateId &&
                !isSubmitting
}

enum class FilterStatus {
    ALL, PENDING, SYNCED
}

class CalfViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = CalfRepository(
        calfDao = database.calfRegistrationDao(),
        syncLogDao = database.syncLogDao()
    )

    val allCalves: StateFlow<List<CalfRegistration>> = repository.allCalves
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCalves: StateFlow<List<CalfRegistration>> = repository.pendingCalves
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncedCalves: StateFlow<List<CalfRegistration>> = repository.syncedCalves
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount: StateFlow<Int> = repository.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val syncedCount: StateFlow<Int> = repository.syncedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCount: StateFlow<Int> = repository.totalCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val knownDamIds: StateFlow<List<String>> = repository.knownDamIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val knownSireIds: StateFlow<List<String>> = repository.knownSireIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncLogs: StateFlow<List<SyncLog>> = repository.syncLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Form State
    private val _formState = MutableStateFlow(
        RegistrationFormState(
            birthDate = getTodayDateString()
        )
    )
    val formState: StateFlow<RegistrationFormState> = _formState.asStateFlow()

    // Sync State
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncResult = MutableStateFlow<SyncResult?>(null)
    val lastSyncResult: StateFlow<SyncResult?> = _lastSyncResult.asStateFlow()

    private val _backendEndpoint = MutableStateFlow("http://192.168.1.100:8080/api/v1/sync")
    val backendEndpoint: StateFlow<String> = _backendEndpoint.asStateFlow()

    // Registry Filter & Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow(FilterStatus.ALL)
    val statusFilter: StateFlow<FilterStatus> = _statusFilter.asStateFlow()

    private val _selectedBreedFilter = MutableStateFlow<String?>(null)
    val selectedBreedFilter: StateFlow<String?> = _selectedBreedFilter.asStateFlow()

    val filteredCalves: StateFlow<List<CalfRegistration>> = combine(
        allCalves,
        _searchQuery,
        _statusFilter,
        _selectedBreedFilter
    ) { calves, query, filter, breed ->
        calves.filter { calf ->
            val matchesQuery = query.isBlank() ||
                    calf.animalId.contains(query, ignoreCase = true) ||
                    calf.breed.contains(query, ignoreCase = true) ||
                    (calf.damId?.contains(query, ignoreCase = true) == true) ||
                    (calf.sireId?.contains(query, ignoreCase = true) == true)

            val matchesStatus = when (filter) {
                FilterStatus.ALL -> true
                FilterStatus.PENDING -> calf.syncStatus == CalfRegistration.SYNC_STATUS_PENDING
                FilterStatus.SYNCED -> calf.syncStatus == CalfRegistration.SYNC_STATUS_SYNCED
            }

            val matchesBreed = breed == null || calf.breed.equals(breed, ignoreCase = true)

            matchesQuery && matchesStatus && matchesBreed
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Calf for detail view
    private val _selectedCalf = MutableStateFlow<CalfRegistration?>(null)
    val selectedCalf: StateFlow<CalfRegistration?> = _selectedCalf.asStateFlow()

    fun selectCalf(calf: CalfRegistration?) {
        _selectedCalf.value = calf
    }

    fun onAnimalIdChanged(newId: String) {
        val trimmed = newId.uppercase(Locale.getDefault())
        _formState.value = _formState.value.copy(
            animalId = trimmed,
            errorMessage = null
        )
        checkDuplicate(trimmed)
    }

    fun onBirthDateChanged(newDate: String) {
        _formState.value = _formState.value.copy(birthDate = newDate, errorMessage = null)
    }

    fun onBreedChanged(newBreed: String) {
        _formState.value = _formState.value.copy(breed = newBreed, errorMessage = null)
    }

    fun onSexChanged(newSex: String) {
        _formState.value = _formState.value.copy(sex = newSex)
    }

    fun onBirthWeightChanged(newWeight: Double?) {
        _formState.value = _formState.value.copy(birthWeightKg = newWeight)
    }

    fun onCalvingEaseChanged(newEase: Int) {
        _formState.value = _formState.value.copy(calvingEase = newEase)
    }

    fun onVigorChanged(newVigor: String) {
        _formState.value = _formState.value.copy(vigor = newVigor)
    }

    fun onHornStatusChanged(newHornStatus: String) {
        _formState.value = _formState.value.copy(hornStatus = newHornStatus)
    }

    fun onPastureLocationChanged(newLocation: String) {
        _formState.value = _formState.value.copy(pastureLocation = newLocation)
    }

    fun onRfidTagChanged(newRfid: String) {
        _formState.value = _formState.value.copy(rfidTag = newRfid)
    }

    fun generateMockRfid() {
        val randomDigits = (100000000000L..999999999999L).random()
        _formState.value = _formState.value.copy(rfidTag = "982 $randomDigits")
    }

    fun generateMockRfidTag() = generateMockRfid()

    fun onDamIdChanged(newDamId: String) {
        _formState.value = _formState.value.copy(damId = newDamId.uppercase(Locale.getDefault()))
    }

    fun onSireIdChanged(newSireId: String) {
        _formState.value = _formState.value.copy(sireId = newSireId.uppercase(Locale.getDefault()))
    }

    fun onPhotoSelected(path: String?) {
        _formState.value = _formState.value.copy(photoPath = path)
    }

    fun setBackendEndpoint(endpoint: String) {
        _backendEndpoint.value = endpoint
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: FilterStatus) {
        _statusFilter.value = filter
    }

    fun setBreedFilter(breed: String?) {
        _selectedBreedFilter.value = breed
    }

    private fun checkDuplicate(id: String) {
        if (id.isBlank()) {
            _formState.value = _formState.value.copy(
                isCheckingDuplicate = false,
                isDuplicateId = false,
                duplicateErrorMessage = null
            )
            return
        }

        viewModelScope.launch {
            _formState.value = _formState.value.copy(isCheckingDuplicate = true)
            val isDuplicate = repository.checkIsDuplicateId(id)
            _formState.value = _formState.value.copy(
                isCheckingDuplicate = false,
                isDuplicateId = isDuplicate,
                duplicateErrorMessage = if (isDuplicate) "Duplicate ID: Animal ID '$id' is already registered on this device" else null
            )
        }
    }

    fun submitRegistration(deviceId: String) {
        val current = _formState.value
        if (!current.isFormValid) return

        viewModelScope.launch {
            _formState.value = current.copy(isSubmitting = true, errorMessage = null)

            val result = repository.registerCalf(
                animalId = current.animalId,
                birthDate = current.birthDate,
                breed = current.breed,
                sex = current.sex,
                birthWeightKg = current.birthWeightKg,
                calvingEase = current.calvingEase,
                vigor = current.vigor,
                hornStatus = current.hornStatus,
                pastureLocation = current.pastureLocation,
                rfidTag = current.rfidTag,
                damId = current.damId,
                sireId = current.sireId,
                photoPath = current.photoPath,
                videoPath = current.videoPath,
                gpsLat = current.mockGpsLat,
                gpsLng = current.mockGpsLng,
                deviceId = deviceId
            )

            when (result) {
                is RegistrationResult.Success -> {
                    _formState.value = RegistrationFormState(
                        birthDate = getTodayDateString(),
                        submissionSuccess = result.calf,
                        generatedGuidPreview = UUID.randomUUID().toString()
                    )
                }
                is RegistrationResult.DuplicateError -> {
                    _formState.value = current.copy(
                        isSubmitting = false,
                        isDuplicateId = true,
                        duplicateErrorMessage = "Animal ID '${result.existingId}' is already registered."
                    )
                }
                is RegistrationResult.ValidationError -> {
                    _formState.value = current.copy(
                        isSubmitting = false,
                        errorMessage = result.message
                    )
                }
                is RegistrationResult.DatabaseError -> {
                    _formState.value = current.copy(
                        isSubmitting = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun clearSubmissionSuccess() {
        _formState.value = _formState.value.copy(submissionSuccess = null)
    }

    fun resetForm() {
        _formState.value = RegistrationFormState(
            birthDate = getTodayDateString(),
            generatedGuidPreview = UUID.randomUUID().toString()
        )
    }

    fun triggerSync() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.triggerSync(_backendEndpoint.value)
            _lastSyncResult.value = result
            _isSyncing.value = false
        }
    }

    fun reSyncRecord(id: Int) {
        viewModelScope.launch {
            repository.markCalfAsPending(id)
        }
    }

    fun getExportCsvString(): String {
        return repository.exportToCsv(allCalves.value)
    }

    companion object {
        fun getTodayDateString(): String {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return formatter.format(Date())
        }
    }
}
