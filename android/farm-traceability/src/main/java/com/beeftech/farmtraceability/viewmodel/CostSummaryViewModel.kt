package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeftech.database.dao.AnimalCostDao
import com.beeftech.database.dao.CostTypeDao
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
    private val animalCostDao: AnimalCostDao,
    private val costTypeDao: CostTypeDao
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

                val totals = animalCostDao.getTotalsByType(animalId)
                    .associate { it.costType to it.total }

                val transportCost = totals[COST_TRANSPORT] ?: 0.0
                val processingCost = totals[COST_PROCESSING] ?: 0.0
                val treatmentCost = totals[COST_TREATMENT] ?: 0.0
                // TODO: feed rows will be derived from LocationFeed in a later phase.
                val feedCost = totals[COST_FEED] ?: 0.0
                val handlingCost = totals[COST_HANDLING] ?: 0.0
                val interestCost = totals[COST_INTEREST] ?: 0.0

                val totalAnimalCost = totals.values.sum()

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

                val allowedTypes = costTypeDao.getActive().map { it.code }

                if (costType !in allowedTypes) {
                    onResult(false, "Invalid cost type.")
                    return@launch
                }

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
        const val COST_TREATMENT = "TREATMENT"
        const val COST_FEED = "FEED"
    }
}
