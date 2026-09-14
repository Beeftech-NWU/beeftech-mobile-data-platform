package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeftech.database.dao.SupplierDao
import com.beeftech.database.entity.Supplier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SupplierViewModel(
    private val supplierDao: SupplierDao
) : ViewModel() {

    private val _suppliers =
        MutableStateFlow<List<Supplier>>(emptyList())

    val suppliers: StateFlow<List<Supplier>> =
        _suppliers.asStateFlow()

    fun loadSuppliers(
        animalId: String
    ) {

        if (animalId.isBlank()) {
            _suppliers.value = emptyList()
            return
        }

        viewModelScope.launch {

            try {

                _suppliers.value =
                    supplierDao.getByAnimalId(
                        animalId
                    )

            } catch (exception: Exception) {

                _suppliers.value =
                    emptyList()
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
            onResult(
                false,
                "Please select an animal first."
            )
            return
        }

        if (supplierName.isBlank()) {
            onResult(
                false,
                "Please enter the supplier name."
            )
            return
        }

        if (glnNumber.isBlank()) {
            onResult(
                false,
                "Please enter the GLN number."
            )
            return
        }

        if (purchaseDate.isBlank()) {
            onResult(
                false,
                "Please enter the purchase date."
            )
            return
        }

        if (purchaseBatchNumber.isBlank()) {
            onResult(
                false,
                "Please enter the purchase batch number."
            )
            return
        }

        viewModelScope.launch {

            try {

                val supplier =
                    Supplier(
                        animalId = animalId,
                        supplierName =
                            supplierName.trim(),
                        glnNumber =
                            glnNumber.trim(),
                        purchaseDate =
                            purchaseDate.trim(),
                        purchaseBatchNumber =
                            purchaseBatchNumber.trim(),
                        timestamp =
                            System.currentTimeMillis()
                    )

                supplierDao.insert(
                    supplier
                )

                _suppliers.value =
                    supplierDao.getByAnimalId(
                        animalId
                    )

                onResult(
                    true,
                    "Supplier record saved successfully."
                )

            } catch (exception: Exception) {

                onResult(
                    false,
                    "Unable to save supplier record."
                )
            }
        }
    }
}