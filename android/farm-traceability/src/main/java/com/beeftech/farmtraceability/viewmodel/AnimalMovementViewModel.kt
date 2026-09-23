package com.beeftech.farmtraceability.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeftech.database.dao.AnimalMovementDao
import com.beeftech.database.entity.AnimalMovementEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnimalMovementViewModel(
    private val animalMovementDao: AnimalMovementDao,
    private val applicationContext: Context
) : ViewModel() {

    private val _movements =
        MutableStateFlow<List<AnimalMovementEntity>>(emptyList())

    val movements: StateFlow<List<AnimalMovementEntity>> =
        _movements.asStateFlow()

    init {
    }

    fun loadMovements(
        animalId: String
    ) {

        if (animalId.isBlank()) {
            _movements.value = emptyList()
            return
        }

        viewModelScope.launch {

            _movements.value =
                animalMovementDao.getByAnimalId(
                    animalId
                )
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
                    AnimalMovementEntity(
                        animalId = animalId,
                        destinationFarmId = movementInformation,
                        destinationPenId = "",
                        movementDate = System.currentTimeMillis().toString(),
                        notes = responsibleWorker
                    )

                animalMovementDao.insert(
                    movement
                )

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
                    "Failed to save movement: ${exception.message}"
                )
            }
        }
    }
}
