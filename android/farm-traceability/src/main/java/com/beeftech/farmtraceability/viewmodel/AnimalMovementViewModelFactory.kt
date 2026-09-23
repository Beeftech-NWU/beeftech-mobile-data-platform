package com.beeftech.farmtraceability.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.beeftech.database.dao.AnimalMovementDao

class AnimalMovementViewModelFactory(
    private val animalMovementDao: AnimalMovementDao,
    context: Context? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                AnimalMovementViewModel::class.java
            )
        ) {

            return AnimalMovementViewModel(
                animalMovementDao = animalMovementDao
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}