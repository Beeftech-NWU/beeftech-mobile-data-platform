package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeftech.database.dao.MortalityDao
import com.beeftech.database.entity.Mortality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MortalityViewModel(
    private val mortalityDao: MortalityDao
) : ViewModel() {

    private val _mortalities =
        MutableStateFlow<List<Mortality>>(emptyList())

    val mortalities: StateFlow<List<Mortality>> =
        _mortalities.asStateFlow()

    fun loadMortalities(
        animalId: String
    ) {

        if (animalId.isBlank()) {
            _mortalities.value = emptyList()
            return
        }

        viewModelScope.launch {

            try {

                _mortalities.value =
                    mortalityDao.getByAnimalId(
                        animalId
                    )

            } catch (exception: Exception) {

                _mortalities.value =
                    emptyList()
            }
        }
    }

    fun saveMortality(
        animalId: String,
        mortalityReason: String,
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

        if (mortalityReason.isBlank()) {

            onResult(
                false,
                "Please enter the mortality reason."
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

                val mortality =
                    Mortality(
                        animalId = animalId,
                        causeOfDeath =
                            mortalityReason.trim(),
                        responsibleWorker =
                            responsibleWorker.trim(),
                        notes = null,
                        timestamp =
                            System.currentTimeMillis()
                    )

                mortalityDao.insert(
                    mortality
                )

                _mortalities.value =
                    mortalityDao.getByAnimalId(
                        animalId
                    )

                onResult(
                    true,
                    "Mortality record saved successfully."
                )

            } catch (exception: Exception) {

                onResult(
                    false,
                    "Unable to save mortality record."
                )
            }
        }
    }
}