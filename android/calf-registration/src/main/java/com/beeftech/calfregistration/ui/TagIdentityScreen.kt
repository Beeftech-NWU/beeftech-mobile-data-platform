package com.beeftech.calfregistration.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeftech.database.util.TagColour
import com.beeftech.database.util.TagNamingUtils
import com.beeftech.tagscanner.ui.EarTagScannerDialog

@Composable
fun TagIdentityScreen(
    formData: CalfRegistrationData,
    onFormDataChange: (CalfRegistrationData) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    onCheckTagDuplicate: (suspend (String) -> Boolean)? = null
) {
    var showScanner by remember { mutableStateOf(false) }

    if (showScanner) {
        EarTagScannerDialog(
            onDismiss = { showScanner = false },
            onTagScanned = { scannedTag ->
                showScanner = false
                val extracted = TagNamingUtils.extractComponents(scannedTag)
                onFormDataChange(
                    formData.copy(
                        tagNumber = extracted?.second ?: scannedTag
                    )
                )
            }
        )
    }

    val focusManager = LocalFocusManager.current
    var selectedColour by remember { mutableStateOf(TagColour.BLUE) }

    // Parse tag number and color
    val extractedComponents = remember(formData.tagNumber) {
        TagNamingUtils.extractComponents(formData.tagNumber)
    }

    LaunchedEffect(extractedComponents) {
        extractedComponents?.first?.let {
            selectedColour = it
        }
    }

    val currentRawInput = extractedComponents?.second ?: formData.tagNumber
    val expandedTag = TagNamingUtils.parseAndExpand(currentRawInput, selectedColour)
    val isTagValid = TagNamingUtils.validateTag(expandedTag)

    var isDuplicateTag by remember { mutableStateOf(false) }
    var isCheckingDuplicate by remember { mutableStateOf(false) }

    LaunchedEffect(expandedTag, isTagValid) {
        if (isTagValid && onCheckTagDuplicate != null) {
            isCheckingDuplicate = true
            isDuplicateTag = onCheckTagDuplicate(expandedTag)
            isCheckingDuplicate = false
        } else {
            isDuplicateTag = false
        }
    }

    fun handleNext() {
        if (isTagValid && !isDuplicateTag) {
            onFormDataChange(formData.copy(tagNumber = expandedTag))
            onNextClick()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BeeftechSurface,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onBackClick != null) {
                                IconButton(onClick = onBackClick) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = "Back",
                                        tint = BeeftechText
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Register calf",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BeeftechText
                                )
                                Text(
                                    text = "Step 1 of 4: Ear tag",
                                    fontSize = 13.sp,
                                    color = BeeftechMutedText
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = Color(0xFFDDEFE4)
                        ) {
                            Text(
                                text = "Step 1",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF17402D)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    StepProgress(currentStep = 1, totalSteps = 4)
                }
            }
        },
        bottomBar = {
            BottomActionDock {
                Button(
                    onClick = { handleNext() },
                    enabled = isTagValid && !isDuplicateTag && !isCheckingDuplicate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BeeftechPrimary,
                        contentColor = BeeftechWhite
                    )
                ) {
                    Text(
                        text = "Next: calf details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BeeftechBackground)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Which ear tag?",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = BeeftechText
            )

            // Tag Color Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (colour in TagColour.entries) {
                    TagColorCard(
                        colour = colour,
                        isSelected = colour == selectedColour,
                        onClick = {
                            selectedColour = colour
                            if (formData.tagNumber.isNotBlank()) {
                                onFormDataChange(
                                    formData.copy(
                                        tagNumber = TagNamingUtils.parseAndExpand(
                                            currentRawInput,
                                            colour
                                        )
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tag Number input field (56dp min height with large readable text)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BeeftechWhite, RoundedCornerShape(14.dp))
                    .border(
                        width = 2.dp,
                        color = if (isDuplicateTag) Color(0xFF8C1D18) else BeeftechPrimary,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Tag number",
                    fontSize = 12.sp,
                    color = BeeftechMutedText,
                    fontWeight = FontWeight.Medium
                )

                OutlinedTextField(
                    value = currentRawInput,
                    onValueChange = { input ->
                        val digitsOrChars = input.filter { it.isLetterOrDigit() }.take(10)
                        onFormDataChange(formData.copy(tagNumber = digitsOrChars))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BeeftechText
                    ),
                    placeholder = {
                        Text(
                            text = "e.g. 14",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = BeeftechBorder
                            )
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent
                    )
                )
            }

            // Real-time inline feedback message
            when {
                currentRawInput.isBlank() -> {
                    Text(
                        text = "Enter the tag sequence (e.g. 14) or scan the tag",
                        fontSize = 13.sp,
                        color = BeeftechMutedText
                    )
                }
                isCheckingDuplicate -> {
                    InlineValidationMessage(
                        message = "Checking tag availability...",
                        isError = false
                    )
                }
                isDuplicateTag -> {
                    InlineValidationMessage(
                        message = "$expandedTag is already registered",
                        isError = true
                    )
                }
                isTagValid -> {
                    InlineValidationMessage(
                        message = "$expandedTag is free to use",
                        isError = false
                    )
                }
                else -> {
                    InlineValidationMessage(
                        message = "Enter a valid numeric or tag sequence",
                        isError = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary Action: Scan tag with camera
            OutlinedButton(
                onClick = { showScanner = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, BeeftechPrimary),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BeeftechPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.DocumentScanner,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scan ear tag instead",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
