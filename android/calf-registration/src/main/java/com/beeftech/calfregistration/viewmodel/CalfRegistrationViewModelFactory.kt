package com.beeftech.calfregistration.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.beeftech.calfregistration.data.CalfRegistrationApiClient
import com.beeftech.calfregistration.data.CalfRegistrationRepository
import com.beeftech.database.dao.CalfRegistrationDao
import com.beeftech.database.repository.PendingSyncRepository

/**
 * Creates [CalfRegistrationViewModel] with the dependencies required
 * for local persistence, immediate sync, and WorkManager background sync.
 */
class CalfRegistrationViewModelFactory(
    context: Context,
    private val calfRegistrationDao: CalfRegistrationDao,
    private val pendingSyncRepository: PendingSyncRepository,
    private val apiClient: CalfRegistrationApiClient =
        CalfRegistrationApiClient()
) : ViewModelProvider.Factory {

    private val applicationContext: Context =
        context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                CalfRegistrationViewModel::class.java
            )
        ) {

            val repository =
                CalfRegistrationRepository(
                    calfRegistrationDao =
                        calfRegistrationDao,
                    pendingSyncRepository =
                        pendingSyncRepository,
                    apiClient =
                        apiClient
                )

            return CalfRegistrationViewModel(
                repository = repository,
                applicationContext = applicationContext
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}