package com.beeftech.calfregistration.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TagIdentityScreen(
    formData: CalfRegistrationData,
    onFormDataChange: (CalfRegistrationData) -> Unit,
    onNextClick: () -> Unit
) {
    var activeLookupField by remember { mutableStateOf<String?>(null) }

    // Dialog for lookup selection
    if (activeLookupField != null) {
        val (title, options, currentVal, onSelect) = when (activeLookupField) {
            "ANIMAL_TYPE" -> Quadruple(
                "Select Animal Type",
                CalfRegistrationLookups.animalTypes,
                formData.animalType
            ) { selected: String ->
                onFormDataChange(formData.copy(animalType = selected))
            }
            "GENDER" -> Quadruple(
                "Select Gender",
                CalfRegistrationLookups.genders,
                formData.gender
            ) { selected: String ->
                onFormDataChange(formData.copy(gender = selected))
            }
            "AGE" -> Quadruple(
                "Select Age",
                CalfRegistrationLookups.ages,
                formData.age
            ) { selected: String ->
                onFormDataChange(formData.copy(age = selected))
            }
            else -> Quadruple(
                "Select Condition",
                CalfRegistrationLookups.conditions,
                formData.condition
            ) { selected: String ->
                onFormDataChange(formData.copy(condition = selected))
            }
        }

        AlertDialog(
            onDismissRequest = { activeLookupField = null },
            title = {
                Text(
                    text = title.uppercase(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BeeftechPrimaryDark
                )
            },
            text = {
                Column {
                    options.forEach { option ->
                        TextButton(
                            onClick = {
                                onSelect(option)
                                activeLookupField = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = option,
                                fontSize = 14.sp,
                                fontWeight = if (option == currentVal) FontWeight.Black else FontWeight.Normal,
                                color = if (option == currentVal) BeeftechAccentRust else BeeftechText
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { activeLookupField = null }) {
                    Text("Cancel", color = BeeftechMutedText)
                }
            },
            containerColor = BeeftechSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeeftechBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        CalfHeader(
            timeString = "07:20",
            unsyncedCount = 1,
            eyebrow = "REGISTER NEW CALF • CONTROL RDP210057",
            title = "CALF IN THE VELD"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            // Row 1: TAG NUMBER & OLD TAG NUMBER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalfTextField(
                    label = "TAG NUMBER",
                    value = formData.tagNumber,
                    onValueChange = { onFormDataChange(formData.copy(tagNumber = it)) },
                    placeholder = "e.g. RMB25423",
                    supportingText = "As on the tag to be inserted",
                    modifier = Modifier.weight(1f)
                )

                CalfTextField(
                    label = "OLD TAG NUMBER",
                    value = formData.oldTagNumber,
                    onValueChange = { onFormDataChange(formData.copy(oldTagNumber = it)) },
                    placeholder = "—",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // TRANSPONDER NUMBER
            CalfTextField(
                label = "TRANSPONDER NUMBER (E-TAG, IF FITTED)",
                value = formData.transponderNumber,
                onValueChange = { onFormDataChange(formData.copy(transponderNumber = it)) },
                placeholder = "40"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // REFERENCE NUMBER
            CalfTextField(
                label = "REFERENCE NUMBER (OPTIONAL)",
                value = formData.referenceNumber,
                onValueChange = { onFormDataChange(formData.copy(referenceNumber = it)) },
                placeholder = "—"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Row 2: ANIMAL TYPE & GENDER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalfLookupDropdownField(
                    label = "ANIMAL TYPE",
                    selectedValue = formData.animalType,
                    onClick = { activeLookupField = "ANIMAL_TYPE" },
                    modifier = Modifier.weight(1f)
                )

                CalfLookupDropdownField(
                    label = "GENDER",
                    selectedValue = formData.gender,
                    onClick = { activeLookupField = "GENDER" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row 3: AGE & CONDITION
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalfLookupDropdownField(
                    label = "AGE",
                    selectedValue = formData.age,
                    onClick = { activeLookupField = "AGE" },
                    modifier = Modifier.weight(1f)
                )

                CalfLookupDropdownField(
                    label = "CONDITION",
                    selectedValue = formData.condition,
                    onClick = { activeLookupField = "CONDITION" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Button -> NEXT
            CalfPrimaryButton(
                text = "NEXT: APPEARANCE & PARENTAGE →",
                onClick = onNextClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Design Note
            CalfDesignNoteCard(
                noteText = "Every 'lookup' field is tagged so the worker knows it's a controlled list, not free text — it can't be mistyped, only mis-selected."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TagIdentityScreenPreview() {
    TagIdentityScreen(
        formData = CalfRegistrationData(),
        onFormDataChange = {},
        onNextClick = {}
    )
}


