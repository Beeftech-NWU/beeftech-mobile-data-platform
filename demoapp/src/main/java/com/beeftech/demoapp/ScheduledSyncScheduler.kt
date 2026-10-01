package com.beeftech.demoapp

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ScheduledSyncScheduler {

    private const val TAG =
        "ScheduledSyncScheduler"

    private const val MORNING_HOUR =
        5

    private const val EVENING_HOUR =
        18

    private const val MORNING_WORK_NAME =
        "beeftech-morning-batch-sync"

    private const val EVENING_WORK_NAME =
        "beeftech-evening-batch-sync"

    /*
     * Previous 15-minute jobs.
     *
     * They are cancelled when the central scheduler is installed
     * so that the client-defined morning/evening schedule becomes
     * the application's periodic safety net.
     */
    private const val LEGACY_CALF_PERIODIC_WORK =
        "calf-registration-periodic-sync"

    private const val LEGACY_TREATMENT_PERIODIC_WORK =
        "treatment_periodic_sync"

    fun schedule(
        context: Context
    ) {

        val applicationContext =
            context.applicationContext

        val workManager =
            WorkManager.getInstance(
                applicationContext
            )

        /*
         * Remove periodic jobs created by older versions.
         *
         * Immediate connectivity-triggered work is NOT cancelled.
         */
        workManager.cancelUniqueWork(
            LEGACY_CALF_PERIODIC_WORK
        )

        workManager.cancelUniqueWork(
            LEGACY_TREATMENT_PERIODIC_WORK
        )

        enqueueDailyWindow(
            workManager = workManager,
            uniqueWorkName = MORNING_WORK_NAME,
            targetHour = MORNING_HOUR,
            label = "05:00-06:00 morning"
        )

        enqueueDailyWindow(
            workManager = workManager,
            uniqueWorkName = EVENING_WORK_NAME,
            targetHour = EVENING_HOUR,
            label = "18:00-19:00 evening"
        )
    }

    private fun enqueueDailyWindow(
        workManager: WorkManager,
        uniqueWorkName: String,
        targetHour: Int,
        label: String
    ) {

        val initialDelayMillis =
            calculateInitialDelayMillis(
                targetHour
            )

        val request =
            PeriodicWorkRequestBuilder<
                ScheduledBatchSyncWorker
            >(
                24,
                TimeUnit.HOURS
            )
                .setInitialDelay(
                    initialDelayMillis,
                    TimeUnit.MILLISECONDS
                )
                .build()

        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        Log.i(
            TAG,
            "Scheduled $label batch sync. " +
                "Initial delay: " +
                TimeUnit.MILLISECONDS
                    .toMinutes(
                        initialDelayMillis
                    ) +
                " minutes."
        )
    }

    private fun calculateInitialDelayMillis(
        targetHour: Int
    ): Long {

        val now =
            Calendar.getInstance()

        val windowStart =
            Calendar.getInstance().apply {

                set(
                    Calendar.HOUR_OF_DAY,
                    targetHour
                )

                set(
                    Calendar.MINUTE,
                    0
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }

        val windowEnd =
            (windowStart.clone() as Calendar).apply {

                add(
                    Calendar.HOUR_OF_DAY,
                    1
                )
            }

        /*
         * If the application starts while we are already inside the
         * client-defined one-hour sync window, make the scheduled work
         * eligible immediately.
         */
        if (
            !now.before(windowStart) &&
            now.before(windowEnd)
        ) {
            return 0L
        }

        val nextRun =
            windowStart.clone() as Calendar

        /*
         * The window for today has already finished.
         * Schedule the next occurrence for tomorrow.
         */
        if (!now.before(windowStart)) {

            nextRun.add(
                Calendar.DAY_OF_YEAR,
                1
            )
        }

        return (
            nextRun.timeInMillis -
                now.timeInMillis
            )
            .coerceAtLeast(0L)
    }
}