package com.beeftech.authentication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeftech.authentication.domain.LoggedInUser
import com.beeftech.authentication.viewmodel.LoginUiState
import com.beeftech.authentication.viewmodel.LoginViewModel

val LoginBackground = Color(0xFFF4F3E8)
val LoginSurface = Color(0xFFFAF9F2)
val LoginPrimary = Color(0xFF4F6256)
val LoginText = Color(0xFF2F3632)
val LoginError = Color(0xFFB00020)

const val PIN_LENGTH = 5

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (LoggedInUser) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    val isBusy = uiState is LoginUiState.Busy

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is LoginUiState.Done) {
            onLoginSuccess(state.user)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = LoginBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "BeefTech Mobile",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = LoginPrimary
            )

            Text(
                text = "Enter your username and PIN",
                fontSize = 14.sp,
                color = LoginText,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    viewModel.clearError()
                },
                label = { Text("Username") },
                singleLine = true,
                enabled = !isBusy,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LoginPrimary,
                    unfocusedBorderColor = LoginText,
                    focusedLabelColor = LoginPrimary,
                    unfocusedLabelColor = LoginText
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Masked PIN dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                for (i in 0 until PIN_LENGTH) {
                    val isEntered = i < pin.length
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (isEntered) LoginPrimary else Color.Transparent)
                            .border(2.dp, LoginPrimary, CircleShape)
                    )
                }
            }

            val errorMessage = (uiState as? LoginUiState.Error)?.message
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = LoginError,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(36.dp))
            }

            if (isBusy) {
                CircularProgressIndicator(
                    color = LoginPrimary,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                PinKeypad(
                    onDigitClick = { digit ->
                        if (pin.length < PIN_LENGTH) {
                            val newPin = pin + digit
                            pin = newPin
                            viewModel.clearError()
                            if (newPin.length == PIN_LENGTH && username.isNotBlank()) {
                                viewModel.login(username, newPin)
                                pin = ""
                            }
                        }
                    },
                    onBackspaceClick = {
                        if (pin.isNotEmpty()) {
                            pin = pin.dropLast(1)
                            viewModel.clearError()
                        }
                    },
                    onClearClick = {
                        pin = ""
                        viewModel.clearError()
                    },
                    enabled = !isBusy
                )
            }
        }
    }
}

@Composable
fun PinKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    enabled: Boolean
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("CLR", "0", "⌫")
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (row in keys) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (key in row) {
                    KeypadButton(
                        text = key,
                        enabled = enabled,
                        onClick = {
                            when (key) {
                                "CLR" -> onClearClick()
                                "⌫" -> onBackspaceClick()
                                else -> onDigitClick(key)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 80.dp, height = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) LoginSurface else Color.LightGray)
            .border(1.dp, LoginPrimary, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = text,
            fontSize = if (text.length == 1) 22.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            color = LoginPrimary
        )
    }
}
