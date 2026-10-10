package com.beeftech.feedcrib.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.beeftech.database.dao.FeedCribDao
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.database.security.TokenProvider
import com.beeftech.feedcrib.data.FeedCribApiClient
import com.beeftech.feedcrib.data.FeedCribCaptureContext
import com.beeftech.feedcrib.data.FeedCribRepository

/**
 * Creates [FeedCribViewModel] with the dependencies required for local persistence, immediate
 * sync, and WorkManager background sync.
 */
class FeedCribViewModelFactory(
    context: Context,
    private val feedCribDao: FeedCribDao,
    private val pendingSyncRepository: PendingSyncRepository,
    private val tokenProvider: TokenProvider,
    private val deviceIdProvider: () -> String,
    private val apiClient: FeedCribApiClient = FeedCribApiClient(tokenProvider = tokenProvider)
) : ViewModelProvider.Factory {

    private val applicationContext: Context = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(FeedCribViewModel::class.java)) {

            val repository = FeedCribRepository(
                feedCribDao = feedCribDao,
                pendingSyncRepository = pendingSyncRepository,
                apiClient = apiClient,
                captureContextProvider = { FeedCribCaptureContext(deviceId = deviceIdProvider()) }
            )

            return FeedCribViewModel(
                repository = repository,
                applicationContext = applicationContext
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
