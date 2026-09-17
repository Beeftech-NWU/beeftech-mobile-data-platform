package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeftech.database.dao.TreatmentDao
import com.beeftech.database.entity.Treatment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TreatmentViewModel(
    private val treatmentDao: TreatmentDao
) : ViewModel() {

    private val _treatments =
        MutableStateFlow<List<Treatment>>(emptyList())

    val treatments: StateFlow<List<Treatment>> =
        _treatments.asStateFlow()

    fun loadTreatments(
        animalId: String
    ) {

        if (animalId.isBlank()) {
            _treatments.value = emptyList()
            return
        }

        viewModelScope.launch {

            try {

                _treatments.value =
                    treatmentDao.getByAnimalId(
                        animalId
                    )

            } catch (exception: Exception) {

                _treatments.value =
                    emptyList()
            }
        }
    }

    fun saveTreatment(
        animalId: String,
        disease: String,
        treatmentName: String,
        batchNumber: String,
        volumeUsed: String,
        costText: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {

        if (animalId.isBlank()) {
            onResult(
                false,
                "Please select an animal first."
            )
            return
        }

        if (disease.isBlank()) {
            onResult(
                false,
                "Please enter the disease."
            )
            return
        }

        if (treatmentName.isBlank()) {
            onResult(
                false,
                "Please enter the treatment."
            )
            return
        }

        if (batchNumber.isBlank()) {
            onResult(
                false,
                "Please enter the batch number."
            )
            return
        }

        if (volumeUsed.isBlank()) {
            onResult(
                false,
                "Please enter the volume used."
            )
            return
        }

        val cleanedCost =
            costText
                .replace("R", "", ignoreCase = true)
                .replace(" ", "")
                .replace(",", ".")
                .trim()

        val cost =
            cleanedCost.toDoubleOrNull()

        if (cost == null || cost < 0) {
            onResult(
                false,
                "Please enter a valid treatment cost."
            )
            return
        }

        viewModelScope.launch {

            try {

                val treatment =
                    Treatment(
                        animalId = animalId,
                        disease = disease.trim(),
                        treatmentName = treatmentName.trim(),
                        batchNumber = batchNumber.trim(),
                        volumeUsed = volumeUsed.trim(),
                        cost = cost,
                        timestamp = System.currentTimeMillis()
                    )

                treatmentDao.insert(
                    treatment
                )

                _treatments.value =
                    treatmentDao.getByAnimalId(
                        animalId
                    )

                onResult(
                    true,
                    "Treatment record saved successfully."
                )

            } catch (exception: Exception) {

                onResult(
                    false,
                    "Unable to save treatment record."
                )
            }
        }
    }
}