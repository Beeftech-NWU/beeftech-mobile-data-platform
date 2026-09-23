package com.beeftech.authentication.ui

import androidx.compose.runtime.Composable
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
    content: @Composable (LoggedInUser) -> Unit
) {
    var currentUser by remember { mutableStateOf(sessionStore.currentUser()) }

    val user = currentUser
    if (user != null && !sessionStore.isExpired()) {
        content(user)
    } else {
        val viewModel: LoginViewModel = viewModel(factory = viewModelFactory)
        LoginScreen(
            viewModel = viewModel,
            onLoginSuccess = { loggedInUser ->
                currentUser = loggedInUser
            }
        )
    }
}
