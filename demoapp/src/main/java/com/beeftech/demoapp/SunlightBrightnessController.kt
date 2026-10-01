package com.beeftech.demoapp

import android.app.Activity
import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import com.beeftech.database.runtime.SunlightUiState

class SunlightBrightnessController(
    private val application: Application
) : SensorEventListener,
    Application.ActivityLifecycleCallbacks {

    private val sensorManager =
        application.getSystemService(
            SensorManager::class.java
        )

    private val lightSensor =
        sensorManager?.getDefaultSensor(
            Sensor.TYPE_LIGHT
        )

    private var resumedActivity: Activity? =
        null

    private var sunlightModeEnabled =
        false

    fun start() {

        SunlightUiState.setSunlightMode(
            false
        )

        application.registerActivityLifecycleCallbacks(
            this
        )

        if (lightSensor == null) {

            Log.i(
                TAG,
                "Ambient light sensor unavailable. " +
                    "System brightness will remain unchanged."
            )

            return
        }

        sensorManager?.registerListener(
            this,
            lightSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        Log.i(
            TAG,
            "Ambient light monitoring started."
        )
    }

    fun stop() {

        sensorManager?.unregisterListener(
            this
        )

        sunlightModeEnabled =
            false

        SunlightUiState.setSunlightMode(
            false
        )

        applyBrightness(
            resumedActivity
        )

        application.unregisterActivityLifecycleCallbacks(
            this
        )

        resumedActivity =
            null
    }

    override fun onSensorChanged(
        event: SensorEvent
    ) {

        if (
            event.sensor.type !=
            Sensor.TYPE_LIGHT
        ) {
            return
        }

        val lux =
            event.values.firstOrNull()
                ?: return

        /*
         * Hysteresis:
         *
         * Enter outdoor mode at 10,000 lux.
         * Remain there until light drops below 5,000 lux.
         *
         * This prevents repeated switching when the sensor
         * sits close to one threshold.
         */
        val shouldEnableSunlightMode =
            if (sunlightModeEnabled) {

                lux >= SUNLIGHT_EXIT_LUX

            } else {

                lux >= SUNLIGHT_ENTER_LUX
            }

        if (
            shouldEnableSunlightMode ==
            sunlightModeEnabled
        ) {
            return
        }

        sunlightModeEnabled =
            shouldEnableSunlightMode

        /*
         * Update the shared state first.
         *
         * Both the demo application theme and the farmer
         * registration theme observe this value.
         */
        SunlightUiState.setSunlightMode(
            sunlightModeEnabled
        )

        applyBrightness(
            resumedActivity
        )

        Log.i(
            TAG,
            "Ambient light: $lux lux. " +
                "Sunlight mode: $sunlightModeEnabled"
        )
    }

    private fun applyBrightness(
        activity: Activity?
    ) {

        activity ?: return

        activity.runOnUiThread {

            val attributes =
                activity.window.attributes

            attributes.screenBrightness =
                if (sunlightModeEnabled) {

                    /*
                     * Maximum per-window brightness.
                     */
                    1.0f

                } else {

                    /*
                     * Return brightness control to Android
                     * and the user's normal system setting.
                     */
                    WindowManager.LayoutParams
                        .BRIGHTNESS_OVERRIDE_NONE
                }

            activity.window.attributes =
                attributes
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) = Unit

    override fun onActivityResumed(
        activity: Activity
    ) {

        resumedActivity =
            activity

        /*
         * Apply sunlight brightness to whichever BeefTech
         * Activity is currently visible.
         */
        applyBrightness(
            activity
        )
    }

    override fun onActivityPaused(
        activity: Activity
    ) {

        if (
            resumedActivity === activity
        ) {
            resumedActivity =
                null
        }
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) = Unit

    override fun onActivityStarted(
        activity: Activity
    ) = Unit

    override fun onActivityStopped(
        activity: Activity
    ) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) = Unit

    override fun onActivityDestroyed(
        activity: Activity
    ) = Unit

    companion object {

        private const val TAG =
            "SunlightBrightness"

        private const val SUNLIGHT_ENTER_LUX =
            10_000f

        private const val SUNLIGHT_EXIT_LUX =
            5_000f
    }
}