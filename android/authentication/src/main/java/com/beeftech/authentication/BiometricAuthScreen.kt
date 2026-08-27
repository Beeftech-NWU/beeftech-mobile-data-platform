package com.beeftech.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity

@Composable
fun BiometricAuthScreen(
    activity: FragmentActivity,
    onAuthSuccess: () -> Unit,
    onFallbackToPin: () -> Unit
) {
    val promptManager = remember { BiometricPromptManager(activity) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "BeefTech Mobile Platform",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (promptManager.canAuthenticate()) {
                    promptManager.showBiometricPrompt(
                        onSuccess = { onAuthSuccess() },
                        onError = { _, _ -> onFallbackToPin() },
                        onFailed = { errorMessage = "Biometric check failed. Try again." }
                    )
                } else {
                    onFallbackToPin()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Unlock with Biometrics")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onFallbackToPin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Use PIN / Password Instead")
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}
