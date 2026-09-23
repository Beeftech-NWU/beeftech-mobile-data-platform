package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.beeftech.database.dao.AnimalCostDao
import com.beeftech.database.dao.TreatmentDao

class CostSummaryViewModelFactory(
    private val treatmentDao: TreatmentDao,
    private val animalCostDao: AnimalCostDao
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                CostSummaryViewModel::class.java
            )
        ) {

            return CostSummaryViewModel(
                treatmentDao = treatmentDao,
                animalCostDao = animalCostDao
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
