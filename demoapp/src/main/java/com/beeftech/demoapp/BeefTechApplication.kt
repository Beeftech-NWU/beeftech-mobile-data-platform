package com.beeftech.demoapp

import android.app.Application
import com.beeftech.authentication.data.EncryptedSessionStore
import com.beeftech.database.BackendConfig
import com.beeftech.database.security.CurrentUserIdRegistry
import com.beeftech.database.security.TokenProviderRegistry

class BeefTechApplication :
    Application() {

    private lateinit var sunlightBrightnessController:
        SunlightBrightnessController

    private lateinit var syncPolicyActivityGuard:
        SyncPolicyActivityGuard

    override fun onCreate() {

        super.onCreate()

        /*
         * Must run before any API client is built, including by
         * WorkManager workers that start without MainActivity.
         */
        BackendConfig.configure(BuildConfig.BACKEND_BASE_URL)

        /*
         * Authentication context is registered at process startup,
         * not only when MainActivity is visible.
         *
         * This is required because WorkManager can start BeefTech
         * in the background for scheduled synchronization.
         */
        val sessionStore =
            EncryptedSessionStore(
                applicationContext
            )

        TokenProviderRegistry.register(
            sessionStore
        )

        CurrentUserIdRegistry.register {

            sessionStore
                .currentUser()
                ?.userId
        }


        /*
         * Enforce persistent Day-7 account locking across all
         * Activities, including Farmer Registration screens that
         * are launched outside MainActivity.
         */
        syncPolicyActivityGuard =
            SyncPolicyActivityGuard()

        registerActivityLifecycleCallbacks(
            syncPolicyActivityGuard
        )


        /*
         * Client-defined morning/evening synchronization.
         */
        ScheduledSyncScheduler.schedule(
            this
        )

        /*
         * Field usability:
         * automatically boost BeefTech screen brightness
         * when the device detects strong ambient sunlight.
         */
        sunlightBrightnessController =
            SunlightBrightnessController(
                this
            )

        sunlightBrightnessController
            .start()
    }
}