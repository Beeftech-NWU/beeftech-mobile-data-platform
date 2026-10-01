package com.beeftech.demoapp

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.repository.SyncPolicyEnforcer
import com.beeftech.database.security.CurrentUserIdRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SyncPolicyActivityGuard :
    Application.ActivityLifecycleCallbacks {

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.Main.immediate
        )


    private var guardedActivity:
            Activity? =
        null


    private var policyJob:
            Job? =
        null


    override fun onActivityResumed(
        activity: Activity
    ) {

        /*
         * MainActivity has the Compose policy gate, which performs
         * both the initial and periodic policy checks itself.
         */
        if (
            activity is MainActivity
        ) {

            cancelGuard()

            return
        }


        cancelGuard()

        guardedActivity =
            activity


        /*
         * Farmer Registration and any future secondary Activities
         * are protected here.
         *
         * This closes the route where an already-open secondary
         * Activity could otherwise remain usable after Day 7.
         */
        policyJob =
            scope.launch {

                while (
                    isActive &&
                    !activity.isFinishing &&
                    !activity.isDestroyed
                ) {

                    val result =
                        evaluateCurrentUser()


                    when (result) {

                        GuardResult.Allowed -> {

                            delay(
                                POLICY_RECHECK_INTERVAL_MS
                            )
                        }


                        GuardResult.Locked,
                        GuardResult.Error -> {

                            returnToMainActivity(
                                activity
                            )

                            return@launch
                        }
                    }
                }
            }
    }


    override fun onActivityPaused(
        activity: Activity
    ) {

        if (
            guardedActivity ===
            activity
        ) {

            cancelGuard()
        }
    }


    override fun onActivityDestroyed(
        activity: Activity
    ) {

        if (
            guardedActivity ===
            activity
        ) {

            cancelGuard()
        }
    }


    private suspend fun evaluateCurrentUser():
            GuardResult {

        val userId =
            CurrentUserIdRegistry
                .currentUserId()
                ?: return GuardResult.Allowed


        return withContext(
            Dispatchers.IO
        ) {

            val database =
                DatabaseProvider
                    .getDatabase()
                    ?: return@withContext GuardResult.Error


            try {

                val evaluation =
                    SyncPolicyEnforcer(
                        pendingSyncDao =
                            database
                                .pendingSyncDao(),

                        syncSecurityDao =
                            database
                                .syncSecurityDao()
                    )
                        .evaluate(
                            userId =
                                userId
                        )


                if (
                    evaluation.accountLocked
                ) {

                    GuardResult.Locked

                } else {

                    GuardResult.Allowed
                }

            } catch (
                cancellation:
                    CancellationException
            ) {

                throw cancellation

            } catch (
                _: Exception
            ) {

                /*
                 * Fail closed.
                 *
                 * Return to MainActivity, whose Compose gate will
                 * either verify the account or display its error UI.
                 */
                GuardResult.Error
            }
        }
    }


    private fun returnToMainActivity(
        activity: Activity
    ) {

        if (
            activity.isFinishing ||
            activity.isDestroyed
        ) {
            return
        }


        val intent =
            Intent(
                activity,
                MainActivity::class.java
            )
                .apply {

                    addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }


        activity.startActivity(
            intent
        )

        activity.finish()
    }


    private fun cancelGuard() {

        policyJob
            ?.cancel()

        policyJob =
            null

        guardedActivity =
            null
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


    private sealed interface GuardResult {

        data object Allowed :
            GuardResult


        data object Locked :
            GuardResult


        data object Error :
            GuardResult
    }


    companion object {

        private const val
                POLICY_RECHECK_INTERVAL_MS =
            60_000L
    }
}
