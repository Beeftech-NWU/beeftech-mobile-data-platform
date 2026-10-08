package com.beeftech.calfregistration.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalfDetailsStepScreen(
    formData: CalfRegistrationData,
    onFormDataChange: (CalfRegistrationData) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var activeLookupField by remember { mutableStateOf<String?>(null) }

    if (activeLookupField != null) {
        val (title, options, currentVal, onSelect) = when (activeLookupField) {
            "ANIMAL_TYPE" -> Quadruple(
                "Select Breed / Animal Type",
                CalfRegistrationLookups.animalTypes,
                formData.animalType
            ) { selected: String ->
                onFormDataChange(formData.copy(animalType = selected))
            }
            "HIDE_COLOUR" -> Quadruple(
                "Select Hide Colour",
                CalfRegistrationLookups.hideColours,
                formData.hideColour
            ) { selected: String ->
                onFormDataChange(formData.copy(hideColour = selected))
            }
            else -> Quadruple(
                "Select Conformity",
                CalfRegistrationLookups.conformities,
                formData.conformity
            ) { selected: String ->
                onFormDataChange(formData.copy(conformity = selected))
            }
        }

        AlertDialog(
            onDismissRequest = { activeLookupField = null },
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = BeeftechText
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    options.forEach { option ->
                        TextButton(
                            onClick = {
                                onSelect(option)
                                activeLookupField = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text(
                                text = option,
                                fontWeight = if (option == currentVal) FontWeight.Bold else FontWeight.Normal,
                                color = if (option == currentVal) BeeftechPrimary else BeeftechText,
                                fontSize = 16.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { activeLookupField = null }) {
                    Text("Cancel", color = BeeftechPrimary)
                }
            },
            containerColor = BeeftechSurface
        )
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
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Back",
                                    tint = BeeftechText
                                )
                            }
                            Column {
                                Text(
                                    text = "Register calf",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BeeftechText
                                )
                                Text(
                                    text = "Step 2 of 4: Calf details",
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
                                text = "Step 2",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF17402D)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    StepProgress(currentStep = 2, totalSteps = 4)
                }
            }
        },
        bottomBar = {
            BottomActionDock {
                Button(
                    onClick = onNextClick,
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
                        text = "Next: age & condition",
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
                text = "Type and appearance",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BeeftechText
            )

            // Breed / Animal type
            CalfLookupDropdownField(
                label = "Breed / Type",
                badgeLabel = "SELECT",
                selectedValue = formData.animalType.ifEmpty { "BRN — Brangus" },
                onClick = { activeLookupField = "ANIMAL_TYPE" }
            )

            // Gender selection - big glove-friendly cards
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "GENDER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BeeftechPrimaryDark
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Female", "Male").forEach { option ->
                        val isSelected = formData.gender.equals(option, ignoreCase = true)
                        Button(
                            onClick = { onFormDataChange(formData.copy(gender = option)) },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = if (isSelected) {
                                ButtonDefaults.buttonColors(
                                    containerColor = BeeftechPrimary,
                                    contentColor = BeeftechWhite
                                )
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = BeeftechWhite,
                                    contentColor = BeeftechText
                                )
                            },
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                        ) {
                            Text(
                                text = option,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Hide colour
            CalfLookupDropdownField(
                label = "Hide Colour",
                badgeLabel = "SELECT",
                selectedValue = formData.hideColour.ifEmpty { "Black" },
                onClick = { activeLookupField = "HIDE_COLOUR" }
            )

            // Conformity
            CalfLookupDropdownField(
                label = "Conformity Score",
                badgeLabel = "SELECT",
                selectedValue = formData.conformity.ifEmpty { "Normal / Good" },
                onClick = { activeLookupField = "CONFORMITY" }
            )
            // Birth details remain in the same CalfRegistrationData record as the other steps.
            Spacer(modifier = Modifier.height(6.dp))
            CalfSectionTitle("Birth Details")
            CalfCard {
                CalfDatePickerField(
                    label = "Birth date",
                    value = formData.birthDate,
                    onValueChange = { chosenDate ->
                        onFormDataChange(formData.copy(birthDate = chosenDate))
                    },
                    infoText = "Select the calf's actual birth date using the calendar."
                )
                Spacer(modifier = Modifier.height(16.dp))
                CalfTextField(
                    label = "Birth mass (kg)",
                    value = formData.birthWeightKg,
                    onValueChange = { raw ->
                        val cleaned = raw.replace(',', '.')
                            .filter { it.isDigit() || it == '.' }
                        val firstDot = cleaned.indexOf('.')
                        val normalized = if (firstDot < 0) cleaned else
                            cleaned.substring(0, firstDot + 1) +
                                cleaned.substring(firstDot + 1).replace(".", "")
                        onFormDataChange(formData.copy(birthWeightKg = normalized))
                    },
                    placeholder = "e.g. 35.0",
                    infoText = "Enter the measured birth mass. If not weighed, use a quick estimate below, and correct it later when measured.",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "QUICK ESTIMATE - ONLY WHEN NOT WEIGHED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BeeftechPrimaryDark,
                        modifier = Modifier.weight(1f)
                    )
                    CalfBirthHelpIcon(
                        title = "Quick Estimate",
                        message = "Only use an estimated birth mass when no measurement is available. The selected value fills in Birth Mass; replace it with a real measurement when available."
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("30" to "Small", "35" to "Typical", "40" to "Large").forEach { (mass, name) ->
                        OutlinedButton(
                            onClick = { onFormDataChange(formData.copy(birthWeightKg = mass)) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 9.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$mass kg", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(name, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
