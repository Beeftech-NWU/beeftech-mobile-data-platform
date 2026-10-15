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
            CalfStepTopBar(
                step = 2,
                title = "Calf details",
                onBackClick = onBackClick
            )
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
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Breed / Animal type
            CalfLookupDropdownField(
                label = "Breed / Type",
                selectedValue = formData.animalType.ifEmpty { "BRN — Brangus" },
                onClick = { activeLookupField = "ANIMAL_TYPE" }
            )

            // Gender selection - glove-friendly 48dp buttons
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CalfFieldLabel("Gender")
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
                                .defaultMinSize(minHeight = 48.dp),
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

            CalfFieldRow {
                CalfLookupDropdownField(
                    label = "Hide Colour",
                    selectedValue = formData.hideColour.ifEmpty { "Black" },
                    onClick = { activeLookupField = "HIDE_COLOUR" },
                    modifier = Modifier.weight(1f)
                )
                CalfLookupDropdownField(
                    label = "Conformity",
                    selectedValue = formData.conformity.ifEmpty { "Normal / Good" },
                    onClick = { activeLookupField = "CONFORMITY" },
                    modifier = Modifier.weight(1f)
                )
            }
            CalfTextField(
                label = "Brand mark (optional)",
                value = formData.mark,
                onValueChange = { onFormDataChange(formData.copy(mark = it.take(40))) },
                placeholder = "Brand or distinguishing mark"
            )
            // Birth details remain in the same CalfRegistrationData record as the other steps.
            Spacer(modifier = Modifier.height(4.dp))
            CalfSectionTitle("Birth Details")
            CalfFieldRow {
                Box(modifier = Modifier.weight(1f)) {
                    CalfDatePickerField(
                        label = "Birth date",
                        value = formData.birthDate,
                        onValueChange = { chosenDate ->
                            onFormDataChange(formData.copy(birthDate = chosenDate))
                        },
                        infoText = "Select the calf's actual birth date using the calendar."
                    )
                }
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
                    infoText = "Enter the measured birth mass.\n\nIf the calf was not weighed, tap a quick estimate " +
                        "(30 kg small, 35 kg typical, 40 kg large) and replace it with a real measurement when available.",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Not weighed?",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = BeeftechMutedText
                )
                listOf("30", "35", "40").forEach { mass ->
                    OutlinedButton(
                        onClick = { onFormDataChange(formData.copy(birthWeightKg = mass)) },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 40.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "$mass kg",
                            fontSize = 13.sp,
                            fontWeight = if (formData.birthWeightKg == mass) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
