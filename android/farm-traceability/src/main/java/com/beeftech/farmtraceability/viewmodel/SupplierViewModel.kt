package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeftech.database.dao.AnimalPurchaseDao
import com.beeftech.database.entity.AnimalPurchaseEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

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
                    purchaseDate = parsePurchaseDate(purchaseDate),
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

    /**
     * "Date of Purchase" is a free-text field with no format enforced on
     * screen, so this accepts either an already-numeric millisecond string
     * or an ISO date the user typed (`yyyy-MM-dd`), falling back to the
     * current time for a blank or unparseable value -- never a fabricated
     * date otherwise.
     */
    private fun parsePurchaseDate(rawValue: String): Long {

        val trimmed = rawValue.trim()

        trimmed.toLongOrNull()?.let { return it }

        listOf("yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd")
            .forEach { pattern ->
                try {
                    val format = SimpleDateFormat(pattern, Locale.US)
                    format.isLenient = false
                    format.parse(trimmed)?.let { return it.time }
                } catch (_: Exception) {
                    // Try the next pattern.
                }
            }

        return System.currentTimeMillis()
    }
}
