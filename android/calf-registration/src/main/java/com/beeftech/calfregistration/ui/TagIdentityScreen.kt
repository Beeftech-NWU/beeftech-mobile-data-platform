package com.beeftech.calfregistration.ui

import androidx.activity.compose.BackHandler
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

    // Going Back (not Next) must still leave the full tag, or Save rejects the short form.
    val handleBack: (() -> Unit)? = onBackClick?.let { back ->
        {
            if (isTagValid && formData.tagNumber != expandedTag) {
                onFormDataChange(formData.copy(tagNumber = expandedTag))
            }
            back()
        }
    }

    if (handleBack != null) {
        BackHandler(onBack = handleBack)
    }

    fun handleNext() {
        if (isTagValid && !isDuplicateTag) {
            onFormDataChange(formData.copy(tagNumber = expandedTag))
            onNextClick()
        }
    }

    Scaffold(
        topBar = {
            CalfStepTopBar(
                step = 1,
                title = "Ear tag",
                onBackClick = handleBack
            )
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
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalfFieldLabel("Tag colour")

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

            // Tag Number input field (large readable text) with the scanner as a trailing action
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BeeftechWhite, RoundedCornerShape(14.dp))
                    .border(
                        width = 2.dp,
                        color = if (isDuplicateTag) Color(0xFF8C1D18) else BeeftechPrimary,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(start = 12.dp, end = 4.dp, top = 6.dp)
            ) {
                Text(
                    text = "Tag number",
                    fontSize = 12.sp,
                    color = BeeftechMutedText,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp)
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
                    trailingIcon = {
                        IconButton(
                            onClick = { showScanner = true },
                            modifier = Modifier
                                .size(48.dp)
                                .background(BeeftechSoftAccent, RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DocumentScanner,
                                contentDescription = "Scan ear tag",
                                tint = BeeftechPrimary
                            )
                        }
                    },
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
                        text = "Enter the tag sequence (e.g. 14) or tap the scan button",
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

            Spacer(modifier = Modifier.height(6.dp))
            CalfSectionTitle("Other identifiers (optional)")
            CalfFieldRow {
                CalfTextField(
                    label = "Old tag number",
                    value = formData.oldTagNumber,
                    onValueChange = { onFormDataChange(formData.copy(oldTagNumber = it.take(MAX_IDENTIFIER_LENGTH))) },
                    placeholder = "Legacy tag",
                    modifier = Modifier.weight(1f)
                )
                CalfTextField(
                    label = "Reference number",
                    value = formData.referenceNumber,
                    onValueChange = { onFormDataChange(formData.copy(referenceNumber = it.take(MAX_IDENTIFIER_LENGTH))) },
                    placeholder = "Optional",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private const val MAX_IDENTIFIER_LENGTH = 40
