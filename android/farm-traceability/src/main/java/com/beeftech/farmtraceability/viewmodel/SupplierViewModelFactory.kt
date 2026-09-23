package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.beeftech.database.dao.AnimalPurchaseDao

class SupplierViewModelFactory(
    private val animalPurchaseDao: AnimalPurchaseDao
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(SupplierViewModel::class.java)) {

            return SupplierViewModel(
                animalPurchaseDao = animalPurchaseDao
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
