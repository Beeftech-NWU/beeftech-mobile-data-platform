package com.beeftech.demoapp

import android.app.Application

class BeefTechApplication :
    Application() {

    private lateinit var sunlightBrightnessController:
        SunlightBrightnessController

    override fun onCreate() {

        super.onCreate()

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