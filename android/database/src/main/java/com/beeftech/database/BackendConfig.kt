package com.beeftech.database

/**
 * Backend base URL shared by every API client. Always ends with a slash.
 *
 * The app sets it once in `Application.onCreate`, before WorkManager can start any sync worker.
 * Clients read it when they are constructed, so an explicit `baseUrl` argument still wins.
 */
object BackendConfig {

    const val HOSTED_BASE_URL = "https://beeftech-backend.onrender.com/"

    @Volatile
    var baseUrl: String = HOSTED_BASE_URL
        private set

    fun configure(url: String) {
        baseUrl = if (url.endsWith("/")) url else "$url/"
    }
}
