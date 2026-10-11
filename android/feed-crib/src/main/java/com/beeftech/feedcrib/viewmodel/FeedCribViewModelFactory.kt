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
    private val apiClient: FeedCribApiClient = FeedCribApiClient(tokenProvider = tokenProvider),
    private val canManageCribs: () -> Boolean = { false },
    private val siteIdProvider: () -> String? = { null }
) : ViewModelProvider.Factory {

    private val applicationContext: Context = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(FeedCribViewModel::class.java)) {

            val repository = FeedCribRepository(
                feedCribDao = feedCribDao,
                pendingSyncRepository = pendingSyncRepository,
                apiClient = apiClient,
                captureContextProvider = { captureContext() },
                canManageCribs = canManageCribs,
                userSiteId = siteIdProvider
            )

            return FeedCribViewModel(
                repository = repository,
                applicationContext = applicationContext,
                canAddCribProvider = canManageCribs
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }

    /** Use a recent permitted fix if available; absence is represented by null, never 0,0. */
    private fun captureContext(): FeedCribCaptureContext {
        val manager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        val location = runCatching {
            if (applicationContext.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
                applicationContext.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) null
            else manager?.getProviders(true)?.mapNotNull { provider ->
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }?.filter {
                val age = android.os.SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos
                age >= 0 && age <= java.util.concurrent.TimeUnit.MINUTES.toNanos(5)
            }?.maxByOrNull { it.elapsedRealtimeNanos }
        }.getOrNull()
        return FeedCribCaptureContext(deviceIdProvider(), gpsLat = location?.latitude, gpsLng = location?.longitude)
    }
}
