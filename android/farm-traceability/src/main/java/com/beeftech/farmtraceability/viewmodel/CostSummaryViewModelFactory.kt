package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.beeftech.database.dao.AnimalCostDao
import com.beeftech.database.dao.CostTypeDao
import com.beeftech.farmtraceability.data.CostRepository

class CostSummaryViewModelFactory(
    private val animalCostDao: AnimalCostDao,
    private val costTypeDao: CostTypeDao,
    private val repository: CostRepository
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
                animalCostDao = animalCostDao,
                costTypeDao = costTypeDao,
                repository = repository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
