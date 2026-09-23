package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeftech.database.dao.AnimalCostDao
import com.beeftech.database.dao.TreatmentDao
import com.beeftech.database.entity.AnimalCost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CostSummaryUiState(
    val transportCost: Double = 0.0,
    val processingCost: Double = 0.0,
    val treatmentCost: Double = 0.0,
    val feedCost: Double = 0.0,
    val handlingCost: Double = 0.0,
    val interestCost: Double = 0.0,
    val totalAnimalCost: Double = 0.0
)

class CostSummaryViewModel(
    private val treatmentDao: TreatmentDao,
    private val animalCostDao: AnimalCostDao
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            CostSummaryUiState()
        )

    val uiState: StateFlow<CostSummaryUiState> =
        _uiState.asStateFlow()

    fun loadCostSummary(
        animalId: String
    ) {

        if (animalId.isBlank()) {
            _uiState.value = CostSummaryUiState()
            return
        }

        viewModelScope.launch {

            try {

                val transportCost =
                    animalCostDao.getTotalByType(
                        animalId,
                        COST_TRANSPORT
                    )

                val processingCost =
                    animalCostDao.getTotalByType(
                        animalId,
                        COST_PROCESSING
                    )

                val treatmentCost =
                    treatmentDao.getTotalCostByAnimalId(
                        animalId
                    )

                val feedCost = 0.0

                val handlingCost =
                    animalCostDao.getTotalByType(
                        animalId,
                        COST_HANDLING
                    )

                val interestCost =
                    animalCostDao.getTotalByType(
                        animalId,
                        COST_INTEREST
                    )

                val totalAnimalCost =
                    transportCost +
                            processingCost +
                            treatmentCost +
                            feedCost +
                            handlingCost +
                            interestCost

                _uiState.value =
                    CostSummaryUiState(
                        transportCost = transportCost,
                        processingCost = processingCost,
                        treatmentCost = treatmentCost,
                        feedCost = feedCost,
                        handlingCost = handlingCost,
                        interestCost = interestCost,
                        totalAnimalCost = totalAnimalCost
                    )

            } catch (exception: Exception) {

                _uiState.value =
                    CostSummaryUiState()
            }
        }
    }

    fun saveCost(
        animalId: String,
        costType: String,
        amountText: String,
        description: String = "",
        gpsLat: Double,
        gpsLng: Double,
        onResult: (
            Boolean,
            String
        ) -> Unit = { _, _ -> }
    ) {

        if (animalId.isBlank()) {
            onResult(false, "Please select an animal first.")
            return
        }

        val allowedTypes = listOf(
            COST_TRANSPORT,
            COST_PROCESSING,
            COST_HANDLING,
            COST_INTEREST
        )

        if (costType !in allowedTypes) {
            onResult(false, "Invalid cost type.")
            return
        }

        val cleanedAmount = amountText
            .replace("R", "", ignoreCase = true)
            .replace(" ", "")
            .replace(",", ".")
            .trim()

        val amount = cleanedAmount.toDoubleOrNull()

        if (amount == null || amount < 0) {
            onResult(false, "Please enter a valid cost amount.")
            return
        }

        viewModelScope.launch {

            try {

                animalCostDao.insert(
                    AnimalCost(
                        animalId = animalId,
                        costType = costType,
                        amount = amount,
                        description = description.trim(),
                        gpsLat = gpsLat,
                        gpsLng = gpsLng,
                        timestamp = System.currentTimeMillis()
                    )
                )

                loadCostSummary(animalId)

                onResult(true, "Cost saved successfully.")

            } catch (exception: Exception) {

                onResult(false, "Unable to save cost.")
            }
        }
    }

    companion object {
        const val COST_TRANSPORT = "TRANSPORT"
        const val COST_PROCESSING = "PROCESSING"
        const val COST_HANDLING = "HANDLING"
        const val COST_INTEREST = "INTEREST"
    }
}
