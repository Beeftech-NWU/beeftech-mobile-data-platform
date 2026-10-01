package com.beeftech.demoapp

import android.app.Application

class BeefTechApplication :
    Application() {

    override fun onCreate() {

        super.onCreate()

        ScheduledSyncScheduler.schedule(
            this
        )
    }
}