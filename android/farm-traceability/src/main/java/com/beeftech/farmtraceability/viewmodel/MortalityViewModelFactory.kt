package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.beeftech.database.dao.MortalityDao

class MortalityViewModelFactory(
    private val mortalityDao: MortalityDao
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                MortalityViewModel::class.java
            )
        ) {

            return MortalityViewModel(
                mortalityDao = mortalityDao
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}