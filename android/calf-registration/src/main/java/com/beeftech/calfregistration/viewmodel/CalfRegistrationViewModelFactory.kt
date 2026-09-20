package com.beeftech.calfregistration.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.beeftech.authentication.domain.TokenProvider
import com.beeftech.calfregistration.data.CalfRegistrationApiClient
import com.beeftech.calfregistration.data.CalfRegistrationRepository
import com.beeftech.database.dao.CalfRegistrationDao
import com.beeftech.database.repository.PendingSyncRepository

/**
 * Mirrors [com.beeftech.farmtraceability.viewmodel.TreatmentViewModelFactory]:
 * takes the DAOs/collaborators needed to build a [CalfRegistrationRepository]
 * and constructs it internally, so callers (e.g. `demoapp`'s `MainActivity`)
 * only need to reach for DAOs obtained from the shared `BeefTechDatabase`.
 *
 * [tokenProvider] is the caller's SessionStore, backing CalfRegistrationApiClient's
 * auth header - callers must have a real logged-in session (i.e. this factory
 * is constructed only from inside AuthGate's authenticated content).
 */
class CalfRegistrationViewModelFactory(
    private val calfRegistrationDao: CalfRegistrationDao,
    private val pendingSyncRepository: PendingSyncRepository,
    private val tokenProvider: TokenProvider,
    private val apiClient: CalfRegistrationApiClient =
        CalfRegistrationApiClient(tokenProvider = tokenProvider)
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                CalfRegistrationViewModel::class.java
            )
        ) {

            return CalfRegistrationViewModel(
                repository = CalfRegistrationRepository(
                    calfRegistrationDao = calfRegistrationDao,
                    pendingSyncRepository = pendingSyncRepository,
                    apiClient = apiClient
                )
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}