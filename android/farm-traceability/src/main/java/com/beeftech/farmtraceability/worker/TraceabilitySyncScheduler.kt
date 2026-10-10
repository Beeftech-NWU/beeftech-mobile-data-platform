package com.beeftech.farmtraceability.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object TraceabilitySyncScheduler {

    @Volatile
    private var applicationContext:
            Context? = null


    fun initialize(
        context: Context
    ) {

        val appContext =
            context.applicationContext

        applicationContext =
            appContext

        schedulePeriodic(
            appContext
        )

        /*
         * Try immediately.
         *
         * When offline the CONNECTED constraint simply makes
         * WorkManager wait until network returns.
         */
        kick()
    }


    fun kick() {

        val context =
            applicationContext
                ?: return

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

        val manager =
            WorkManager
                .getInstance(
                    context
                )


        manager.enqueueUniqueWork(
            "traceability-outbox-network-sync",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<
                TraceabilityOutboxWorker
                >()
                .setConstraints(
                    constraints
                )
                .build()
        )


        manager.enqueueUniqueWork(
            "farmer-animal-link-sync",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<FarmerAnimalLinkSyncWorker>()
                .setConstraints(constraints)
                .build()
        )

        manager.enqueueUniqueWork(
            "traceability-movement-network-sync",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<
                AnimalMovementSyncWorker
                >()
                .setConstraints(
                    constraints
                )
                .build()
        )


        manager.enqueueUniqueWork(
            "traceability-treatment-network-sync",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<
                TreatmentSyncWorker
                >()
                .setConstraints(
                    constraints
                )
                .build()
        )
    }


    private fun schedulePeriodic(
        context: Context
    ) {

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

        val manager =
            WorkManager
                .getInstance(
                    context
                )


        manager.enqueueUniquePeriodicWork(
            "traceability-outbox-periodic-sync",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<
                TraceabilityOutboxWorker
                >(
                15,
                TimeUnit.MINUTES
            )
                .setConstraints(
                    constraints
                )
                .build()
        )


        manager.enqueueUniquePeriodicWork(
            "traceability-movement-periodic-sync",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<
                AnimalMovementSyncWorker
                >(
                15,
                TimeUnit.MINUTES
            )
                .setConstraints(
                    constraints
                )
                .build()
        )


        manager.enqueueUniquePeriodicWork(
            "farmer-animal-link-periodic-sync",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<FarmerAnimalLinkSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
        )

        manager.enqueueUniquePeriodicWork(
            "traceability-treatment-periodic-sync",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<
                TreatmentSyncWorker
                >(
                15,
                TimeUnit.MINUTES
            )
                .setConstraints(
                    constraints
                )
                .build()
        )
    }
}
