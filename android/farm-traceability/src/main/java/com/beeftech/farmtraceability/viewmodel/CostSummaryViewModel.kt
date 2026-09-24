package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeftech.database.dao.AnimalCostDao
import com.beeftech.database.entity.AnimalCost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI values are projections of animal_costs rows; none are persisted totals. */
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
    private val animalCostDao: AnimalCostDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(CostSummaryUiState())
    val uiState: StateFlow<CostSummaryUiState> = _uiState.asStateFlow()

    fun loadCostSummary(animalId: String) {
        if (animalId.isBlank()) {
            _uiState.value = CostSummaryUiState()
            return
        }

        viewModelScope.launch {
            try {
                val totals = animalCostDao.getTotalsByType(animalId)
                    .associate { it.costType to it.total }

                _uiState.value = CostSummaryUiState(
                    transportCost = totals[COST_TRANSPORT] ?: 0.0,
                    processingCost = totals[COST_PROCESSING] ?: 0.0,
                    treatmentCost = totals[COST_TREATMENT] ?: 0.0,
                    feedCost = totals[COST_FEED] ?: 0.0,
                    handlingCost = totals[COST_HANDLING] ?: 0.0,
                    interestCost = totals[COST_INTEREST] ?: 0.0,
                    totalAnimalCost = animalCostDao.getTotalForAnimal(animalId)
                )
            } catch (_: Exception) {
                _uiState.value = CostSummaryUiState()
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
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (animalId.isBlank()) {
            onResult(false, "Please select an animal first.")
            return
        }

        val amount = amountText
            .replace("R", "", ignoreCase = true)
            .replace(" ", "")
            .replace(",", ".")
            .trim()
            .toDoubleOrNull()

        if (amount == null || amount < 0 || costType.isBlank()) {
            onResult(false, "Please enter a valid cost amount and category.")
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
            } catch (_: Exception) {
                onResult(false, "Unable to save cost.")
            }
        }
    }

    companion object {
        const val COST_TRANSPORT = "TRANSPORT"
        const val COST_PROCESSING = "PROCESSING"
        const val COST_TREATMENT = "TREATMENT"
        const val COST_FEED = "FEED"
        const val COST_HANDLING = "HANDLING"
        const val COST_INTEREST = "INTEREST"
    }
}
