package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeftech.database.entity.CalfRegistration
import com.beeftech.farmtraceability.repository.FindAnimalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FindAnimalUiState {

    data object Idle : FindAnimalUiState

    data object Loading : FindAnimalUiState

    data class Found(
        val animal: CalfRegistration
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

    fun findAnimal(animalReference: String) {

        val reference = animalReference
            .trim()
            .uppercase()

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