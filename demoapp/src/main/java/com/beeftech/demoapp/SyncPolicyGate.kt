package com.beeftech.demoapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.beeftech.authentication.data.SessionStore
import com.beeftech.authentication.domain.LoggedInUser
import com.beeftech.authentication.ui.AuthGate
import com.beeftech.authentication.viewmodel.LoginViewModelFactory
import com.beeftech.database.dao.SyncSecurityDao
import com.beeftech.database.repository.SyncPolicyEnforcer
import com.beeftech.database.repository.SyncPolicyEvaluation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext


@Composable
fun PolicyAwareAuthGate(
    sessionStore: SessionStore,
    viewModelFactory: LoginViewModelFactory,
    syncPolicyEnforcer: SyncPolicyEnforcer,
    content:
        @Composable (
            user: LoggedInUser,
            onLogout: () -> Unit
        ) -> Unit
) {

    AuthGate(
        sessionStore =
            sessionStore,

        viewModelFactory =
            viewModelFactory
    ) {
            loggedInUser,
            onLogout ->

        SyncPolicyGate(
            user =
                loggedInUser,

            syncPolicyEnforcer =
                syncPolicyEnforcer,

            onLogout =
                onLogout
        ) {

            content(
                loggedInUser,
                onLogout
            )
        }
    }
}


@Composable
private fun SyncPolicyGate(
    user: LoggedInUser,
    syncPolicyEnforcer: SyncPolicyEnforcer,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {

    var retryKey by
        remember(
            user.userId
        ) {
            mutableIntStateOf(0)
        }


    var state by
        remember(
            user.userId
        ) {
            mutableStateOf<SyncPolicyGateState>(
                SyncPolicyGateState.Checking
            )
        }


    /*
     * Evaluate immediately after login / session restoration.
     *
     * While MainActivity remains open we also re-check periodically,
     * so an account cannot remain inside the app indefinitely after
     * its oldest unsynced record crosses the Day-7 boundary.
     */
    LaunchedEffect(
        user.userId,
        retryKey
    ) {

        state =
            SyncPolicyGateState.Checking


        while (isActive) {

            val evaluation =
                try {

                    withContext(
                        Dispatchers.IO
                    ) {

                        syncPolicyEnforcer
                            .evaluate(
                                userId =
                                    user.userId
                            )
                    }

                } catch (
                    cancellation:
                        CancellationException
                ) {

                    throw cancellation

                } catch (
                    error: Exception
                ) {

                    state =
                        SyncPolicyGateState.Error(
                            message =
                                error.message
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?: "Account security status could not be verified."
                        )

                    break
                }


            if (
                evaluation.accountLocked
            ) {

                state =
                    SyncPolicyGateState.Locked(
                        evaluation
                    )

                break
            }


            state =
                SyncPolicyGateState.Allowed(
                    evaluation
                )


            delay(
                POLICY_RECHECK_INTERVAL_MS
            )
        }
    }


    when (
        val currentState =
            state
    ) {

        SyncPolicyGateState.Checking -> {

            PolicyCheckingScreen()
        }


        is SyncPolicyGateState.Allowed -> {

            content()
        }


        is SyncPolicyGateState.Locked -> {

            Day7AccountLockedScreen(
                username =
                    user.username,

                onLogout =
                    onLogout
            )
        }


        is SyncPolicyGateState.Error -> {

            PolicyVerificationErrorScreen(
                message =
                    currentState.message,

                onRetry = {

                    retryKey++
                },

                onLogout =
                    onLogout
            )
        }
    }
}


@Composable
private fun PolicyCheckingScreen() {

    Box(
        modifier =
            Modifier.fillMaxSize(),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {

            CircularProgressIndicator()

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )

            Text(
                text =
                    "Checking account security?"
            )
        }
    }
}


@Composable
private fun Day7AccountLockedScreen(
    username: String,
    onLogout: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    24.dp
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text =
                    "Account locked",

                style =
                    MaterialTheme
                        .typography
                        .headlineMedium,

                color =
                    MaterialTheme
                        .colorScheme
                        .error,

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            Text(
                text =
                    username,

                style =
                    MaterialTheme
                        .typography
                        .titleMedium,

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )


            Text(
                text =
                    SyncSecurityDao
                        .DAY_7_LOCK_REASON,

                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            Text(
                text =
                    "The account cannot continue into BeefTech until an administrator restores access.",

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )


            Button(
                onClick =
                    onLogout,

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    text =
                        "Log out / switch account"
                )
            }
        }
    }
}


@Composable
private fun PolicyVerificationErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onLogout: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    24.dp
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    "Unable to verify account security",

                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            Text(
                text =
                    message,

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )


            Button(
                modifier =
                    Modifier.fillMaxWidth(),

                onClick =
                    onRetry
            ) {

                Text(
                    text =
                        "Try again"
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            OutlinedButton(
                modifier =
                    Modifier.fillMaxWidth(),

                onClick =
                    onLogout
            ) {

                Text(
                    text =
                        "Log out"
                )
            }
        }
    }
}


private sealed interface SyncPolicyGateState {

    data object Checking :
        SyncPolicyGateState


    data class Allowed(
        val evaluation:
            SyncPolicyEvaluation
    ) :
        SyncPolicyGateState


    data class Locked(
        val evaluation:
            SyncPolicyEvaluation
    ) :
        SyncPolicyGateState


    data class Error(
        val message:
            String
    ) :
        SyncPolicyGateState
}


private const val POLICY_RECHECK_INTERVAL_MS =
    60_000L
