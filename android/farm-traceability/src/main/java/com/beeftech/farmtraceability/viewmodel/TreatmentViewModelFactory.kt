package com.beeftech.farmtraceability.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.beeftech.farmtraceability.data.TreatmentRepository

class TreatmentViewModelFactory(
    private val repository: TreatmentRepository,
    private val applicationContext: Context
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
                repository =
                    repository,

                applicationContext =
                    applicationContext
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}