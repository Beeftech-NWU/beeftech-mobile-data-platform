package com.beeftech.authentication.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeftech.authentication.domain.LoggedInUser
import com.beeftech.authentication.viewmodel.LoginUiState
import com.beeftech.authentication.viewmodel.LoginViewModel

val LoginBackground = Color(0xFFFAF9F2)
val LoginSurface = Color(0xFFFFFFFF)
val LoginPrimary = Color(0xFF3E5D4D)
val LoginPrimaryStrong = Color(0xFF294436)
val LoginSoft = Color(0xFFE6F1EA)
val LoginText = Color(0xFF1F2823)
val LoginMuted = Color(0xFF6D756F)
val LoginBorder = Color(0xFFD9DDD8)
val LoginError = Color(0xFFB23A35)

const val PIN_LENGTH = 5

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (LoggedInUser) -> Unit,
    modifier: Modifier = Modifier,
    notice: String? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showForgotPin by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isBusy = uiState is LoginUiState.Busy

    fun submit() {
        if (!isBusy && username.isNotBlank() && pin.length == PIN_LENGTH) {
            keyboardController?.hide()
            viewModel.login(username.trim(), pin)
        }
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is LoginUiState.Done) onLoginSuccess(state.user)
    }

    if (showForgotPin) {
        AlertDialog(
            onDismissRequest = { showForgotPin = false },
            title = { Text("Forgot your PIN?") },
            text = {
                Text(
                    "Ask your BeefTech administrator or manager to reset your 5-digit PIN. " +
                        "Your locally saved farm records will stay on this device."
                )
            },
            confirmButton = {
                TextButton(onClick = { showForgotPin = false }) { Text("OK") }
            }
        )
    }

    Surface(modifier = modifier.fillMaxSize(), color = LoginBackground) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(LoginPrimary)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = LoginSurface),
                    border = BorderStroke(1.dp, LoginBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = LoginSoft,
                            border = BorderStroke(1.dp, LoginBorder)
                        ) {
                            Text(
                                text = "🐄",
                                fontSize = 46.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "BEEFTECH",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            color = LoginPrimaryStrong
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Sign in to your account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = LoginText
                        )

                        Text(
                            text = "Secure access to your farm records",
                            style = MaterialTheme.typography.bodySmall,
                            color = LoginMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        )

                        notice?.let {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = LoginSoft
                            ) {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LoginPrimaryStrong,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                viewModel.clearError()
                            },
                            label = { Text("Username") },
                            placeholder = { Text("Enter username") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Person, contentDescription = null)
                            },
                            singleLine = true,
                            enabled = !isBusy,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LoginPrimary,
                                unfocusedBorderColor = LoginBorder,
                                focusedLabelColor = LoginPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = pin,
                            onValueChange = { value ->
                                pin = value.filter(Char::isDigit).take(PIN_LENGTH)
                                viewModel.clearError()
                            },
                            label = { Text("PIN") },
                            placeholder = { Text("5-digit PIN") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Lock, contentDescription = null)
                            },
                            singleLine = true,
                            enabled = !isBusy,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LoginPrimary,
                                unfocusedBorderColor = LoginBorder,
                                focusedLabelColor = LoginPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        val errorMessage = (uiState as? LoginUiState.Error)?.message
                        if (errorMessage != null) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFE8E6)
                            ) {
                                Text(
                                    text = errorMessage,
                                    color = LoginError,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { submit() },
                            enabled = !isBusy && username.isNotBlank() && pin.length == PIN_LENGTH,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LoginPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.size(10.dp))
                                Text("Signing in…")
                            } else {
                                Text("Sign In", fontWeight = FontWeight.Bold)
                            }
                        }

                        TextButton(onClick = { showForgotPin = true }) {
                            Text("Forgot PIN?", color = LoginPrimaryStrong)
                        }

                        Text(
                            text = "Offline work is saved securely and syncs automatically when internet returns.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LoginMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/* Kept for API compatibility with any previews/tests that still call it. */
@Composable
fun PinKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    enabled: Boolean
) = Unit

@Composable
fun KeypadButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) = Unit
