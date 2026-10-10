package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeftech.database.dao.CalfRegistrationView
import com.beeftech.farmtraceability.repository.FindAnimalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FindAnimalUiState {

    data object Idle : FindAnimalUiState

    data object Loading : FindAnimalUiState

    data class Found(
        val animal: CalfRegistrationView
    ) : FindAnimalUiState

    data class NotFound(
        val animalReference: String
    ) : FindAnimalUiState

    data class Error(
        val message: String
    ) : FindAnimalUiState
}

class FindAnimalViewModel(
    private val repository: FindAnimalRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<FindAnimalUiState>(FindAnimalUiState.Idle)

    val uiState: StateFlow<FindAnimalUiState> =
        _uiState.asStateFlow()

    /** Registered animals for the search list; null until the first load completes. */
    val animals: StateFlow<List<CalfRegistrationView>?> =
        repository.observeAnimals()
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectAnimal(animal: CalfRegistrationView) {
        _uiState.value = FindAnimalUiState.Found(animal)
    }

    fun findAnimal(animalReference: String) {

        val reference = animalReference.trim()

        if (reference.isBlank()) {
            _uiState.value =
                FindAnimalUiState.Error("Please enter an animal reference.")
            return
        }

        viewModelScope.launch {

            _uiState.value = FindAnimalUiState.Loading

            try {

                val animal = repository.findAnimal(reference)

                _uiState.value =
                    if (animal != null) {
                        FindAnimalUiState.Found(animal)
                    } else {
                        FindAnimalUiState.NotFound(reference)
                    }

            } catch (exception: Exception) {

                _uiState.value =
                    FindAnimalUiState.Error(
                        exception.message
                            ?: "Unable to find the animal."
                    )
            }
        }
    }

    fun resetState() {
        _uiState.value = FindAnimalUiState.Idle
    }
}
