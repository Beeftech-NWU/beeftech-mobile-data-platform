package com.beeftech.farmtraceability.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.beeftech.database.dao.AnimalMovementDao
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.farmtraceability.data.AnimalMovementApiClient
import com.beeftech.farmtraceability.data.AnimalMovementRepository

class AnimalMovementViewModelFactory(
    private val animalMovementDao: AnimalMovementDao,
    private val pendingSyncRepository: PendingSyncRepository,
    context: Context,
    private val apiClient: AnimalMovementApiClient =
        AnimalMovementApiClient()
) : ViewModelProvider.Factory {

    private val applicationContext =
        context.applicationContext

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
                animalMovementDao = animalMovementDao,
                applicationContext = applicationContext
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}