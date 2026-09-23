package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeftech.database.dao.AnimalPurchaseDao
import com.beeftech.database.entity.AnimalPurchaseEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SupplierViewModel(
    private val animalPurchaseDao: AnimalPurchaseDao
) : ViewModel() {

    private val _purchases =
        MutableStateFlow<List<AnimalPurchaseEntity>>(emptyList())

    val purchases: StateFlow<List<AnimalPurchaseEntity>> =
        _purchases.asStateFlow()

    val suppliers: StateFlow<List<AnimalPurchaseEntity>> get() = purchases

    fun loadSuppliers(
        animalId: String
    ) {

        if (animalId.isBlank()) {
            _purchases.value = emptyList()
            return
        }

        viewModelScope.launch {

            try {

                animalPurchaseDao.getPurchasesForAnimal(animalId)
                    .collect { list ->
                        _purchases.value = list
                    }

            } catch (exception: Exception) {

                _purchases.value = emptyList()
            }
        }
    }

    fun saveSupplier(
        animalId: String,
        supplierName: String,
        glnNumber: String,
        purchaseDate: String,
        purchaseBatchNumber: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {

        if (animalId.isBlank()) {
            onResult(false, "Please select an animal first.")
            return
        }

        if (supplierName.isBlank()) {
            onResult(false, "Please enter the seller/supplier name.")
            return
        }

        viewModelScope.launch {

            try {

                val purchase = AnimalPurchaseEntity(
                    animalId = animalId,
                    purchasePrice = 0.0,
                    purchaseDate = purchaseDate.ifBlank { System.currentTimeMillis().toString() },
                    sellerName = supplierName.trim(),
                    notes = "GLN: $glnNumber, Batch: $purchaseBatchNumber"
                )

                animalPurchaseDao.insertPurchase(purchase)

                onResult(true, "Purchase record saved successfully.")

            } catch (exception: Exception) {

                onResult(false, "Unable to save purchase record.")
            }
        }
    }
}
