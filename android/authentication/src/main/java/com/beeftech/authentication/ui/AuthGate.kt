package com.beeftech.authentication.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beeftech.authentication.data.SessionStore
import com.beeftech.authentication.domain.LoggedInUser
import com.beeftech.authentication.viewmodel.LoginViewModel
import com.beeftech.authentication.viewmodel.LoginViewModelFactory

@Composable
fun AuthGate(
    sessionStore: SessionStore,
    viewModelFactory: LoginViewModelFactory,
    content: @Composable (user: LoggedInUser, onLogout: () -> Unit) -> Unit
) {
    var currentUser by remember { mutableStateOf(sessionStore.currentUser()) }
    var notice by remember { mutableStateOf<String?>(null) }
    val viewModel: LoginViewModel = viewModel(factory = viewModelFactory)

    /* The server ended the session (deactivated, PIN reset, unbound or revoked phone): back to login. */
    LaunchedEffect(sessionStore) {
        sessionStore.sessionEvents.collect {
            viewModel.logout()
            currentUser = null
            notice = "Your session has ended. Sign in again."
        }
    }

    val user = currentUser
    if (user != null && !sessionStore.isExpired()) {
        content(user) {
            viewModel.logout()
            currentUser = null
        }
    } else {
        LoginScreen(
            viewModel = viewModel,
            notice = notice,
            onLoginSuccess = { loggedInUser ->
                notice = null
                currentUser = loggedInUser
            }
        )
    }
}
