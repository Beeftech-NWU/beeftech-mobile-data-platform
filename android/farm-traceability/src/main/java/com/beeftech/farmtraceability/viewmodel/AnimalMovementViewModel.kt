package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeftech.database.dao.AnimalMovementDao
import com.beeftech.database.entity.AnimalMovement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnimalMovementViewModel(
    private val animalMovementDao: AnimalMovementDao
) : ViewModel() {

    private val _movements =
        MutableStateFlow<List<AnimalMovement>>(emptyList())

    val movements: StateFlow<List<AnimalMovement>> =
        _movements.asStateFlow()

    fun loadMovements(
        animalId: String
    ) {

        if (animalId.isBlank()) {
            _movements.value = emptyList()
            return
        }

        viewModelScope.launch {

            try {

                _movements.value =
                    animalMovementDao.getByAnimalId(
                        animalId
                    )

            } catch (exception: Exception) {

                _movements.value =
                    emptyList()
            }
        }
    }

    fun saveMovement(
        animalId: String,
        movementInformation: String,
        responsibleWorker: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {

        if (animalId.isBlank()) {
            onResult(
                false,
                "Please select an animal first."
            )
            return
        }

        if (movementInformation.isBlank()) {
            onResult(
                false,
                "Please enter movement information."
            )
            return
        }

        if (responsibleWorker.isBlank()) {
            onResult(
                false,
                "Please enter the responsible worker."
            )
            return
        }

        viewModelScope.launch {

            try {

                val movement =
                    AnimalMovement(
                        animalId = animalId,
                        movementType = movementInformation,
                        responsibleWorker = responsibleWorker,
                        timestamp = System.currentTimeMillis()
                    )

                animalMovementDao.insert(
                    movement
                )

                // Reload the selected animal's movement history
                // immediately after saving.
                _movements.value =
                    animalMovementDao.getByAnimalId(
                        animalId
                    )

                onResult(
                    true,
                    "Movement record saved successfully."
                )

            } catch (exception: Exception) {

                onResult(
                    false,
                    "Unable to save movement record."
                )
            }
        }
    }
}