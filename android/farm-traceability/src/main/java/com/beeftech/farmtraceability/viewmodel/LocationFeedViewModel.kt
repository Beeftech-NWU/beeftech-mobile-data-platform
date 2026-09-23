package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeftech.database.dao.AnimalMovementDao
import com.beeftech.database.entity.AnimalMovementEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationFeedViewModel(
    private val animalMovementDao: AnimalMovementDao
) : ViewModel() {

    private val _records =
        MutableStateFlow<List<AnimalMovementEntity>>(
            emptyList()
        )

    val records: StateFlow<List<AnimalMovementEntity>> =
        _records.asStateFlow()

    fun loadRecords(
        animalId: String
    ) {

        if (animalId.isBlank()) {
            _records.value = emptyList()
            return
        }

        viewModelScope.launch {

            try {

                _records.value =
                    animalMovementDao.getByAnimalId(
                        animalId
                    )

            } catch (exception: Exception) {

                _records.value = emptyList()
            }
        }
    }

    fun saveRecord(
        animalId: String,
        destination: String,
        daysInDestinationText: String,
        rationName: String,
        rationDaysText: String,
        rationCostText: String,
        onResult: (
            Boolean,
            String
        ) -> Unit = { _, _ -> }
    ) {

        if (animalId.isBlank()) {
            onResult(false, "Please select an animal first.")
            return
        }

        if (destination.isBlank()) {
            onResult(false, "Please enter the destination.")
            return
        }

        viewModelScope.launch {

            try {

                val record =
                    AnimalMovementEntity(
                        animalId = animalId,
                        destinationFarmId = destination.trim(),
                        destinationPenId = "",
                        movementDate = System.currentTimeMillis().toString(),
                        feedLocationType = rationName.trim(),
                        notes = "Days: $daysInDestinationText, Ration days: $rationDaysText, Cost: $rationCostText"
                    )

                animalMovementDao.insert(record)

                _records.value =
                    animalMovementDao.getByAnimalId(animalId)

                onResult(
                    true,
                    "Location and feed record saved successfully."
                )

            } catch (exception: Exception) {

                onResult(
                    false,
                    "Unable to save location and feed record."
                )
            }
        }
    }
}
