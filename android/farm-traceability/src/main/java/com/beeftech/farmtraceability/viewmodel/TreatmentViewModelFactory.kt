package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.beeftech.database.dao.TreatmentDao

class TreatmentViewModelFactory(
    private val treatmentDao: TreatmentDao
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                TreatmentViewModel::class.java
            )
        ) {

            return TreatmentViewModel(
                treatmentDao = treatmentDao
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}