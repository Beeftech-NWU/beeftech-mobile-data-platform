package com.beeftech.calfregistration.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

    if (activeLookupField != null) {
        val (title, options, currentVal, onSelect) = when (activeLookupField) {
            "ANIMAL_TYPE" -> Quadruple("Select Animal Type", CalfRegistrationLookups.animalTypes, formData.animalType) { selected: String ->
                onFormDataChange(formData.copy(animalType = selected))
            }
            "GENDER" -> Quadruple("Select Gender", CalfRegistrationLookups.genders, formData.gender) { selected: String ->
                onFormDataChange(formData.copy(gender = selected))
            }
            "AGE" -> Quadruple("Select Age", CalfRegistrationLookups.ages, formData.age) { selected: String ->
                onFormDataChange(formData.copy(age = selected))
            }
            else -> Quadruple("Select Condition", CalfRegistrationLookups.conditions, formData.condition) { selected: String ->
                onFormDataChange(formData.copy(condition = selected))
            }
        }

        AlertDialog(
            onDismissRequest = { activeLookupField = null },
            title = { Text(title, fontWeight = FontWeight.Bold, color = BeeftechPrimaryDark) },
            text = {
                Column {
                    options.forEach { option ->
                        TextButton(
                            onClick = { onSelect(option); activeLookupField = null },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = option,
                                fontWeight = if (option == currentVal) FontWeight.Bold else FontWeight.Normal,
                                color = if (option == currentVal) BeeftechPrimaryDark else BeeftechText
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { activeLookupField = null }) { Text("Cancel") } },
            containerColor = BeeftechSurface
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BeeftechBackground).verticalScroll(rememberScrollState())
    ) {
        CalfHeader(
            eyebrow = "Calf registration",
            title = "Register Calf",
            subtitle = "Capture the calf's tag and identity details.",
            icon = Icons.Outlined.Pets
        )

        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            CalfSectionTitle("Tag identity")
            Spacer(modifier = Modifier.height(12.dp))
            CalfCard {
                CalfTextField(
                    label = "Tag number",
                    value = formData.tagNumber,
                    onValueChange = { onFormDataChange(formData.copy(tagNumber = it)) },
                    placeholder = "e.g. RMB25423",
                    supportingText = "As shown on the tag to be inserted"
                )
                Spacer(modifier = Modifier.height(16.dp))
                CalfTextField(
                    label = "Old tag number",
                    value = formData.oldTagNumber,
                    onValueChange = { onFormDataChange(formData.copy(oldTagNumber = it)) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                CalfTextField(
                    label = "Transponder number",
                    value = formData.transponderNumber,
                    onValueChange = { onFormDataChange(formData.copy(transponderNumber = it)) },
                    placeholder = "E-tag, if fitted"
                )
                Spacer(modifier = Modifier.height(16.dp))
                CalfTextField(
                    label = "Reference number",
                    value = formData.referenceNumber,
                    onValueChange = { onFormDataChange(formData.copy(referenceNumber = it)) },
                    placeholder = "Optional"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            CalfSectionTitle("Calf details")
            Spacer(modifier = Modifier.height(12.dp))
            CalfCard {
                CalfLookupDropdownField("Animal type", selectedValue = formData.animalType, onClick = { activeLookupField = "ANIMAL_TYPE" })
                Spacer(modifier = Modifier.height(16.dp))
                CalfLookupDropdownField("Gender", selectedValue = formData.gender, onClick = { activeLookupField = "GENDER" })
                Spacer(modifier = Modifier.height(16.dp))
                CalfLookupDropdownField("Age", selectedValue = formData.age, onClick = { activeLookupField = "AGE" })
                Spacer(modifier = Modifier.height(16.dp))
                CalfLookupDropdownField("Condition", selectedValue = formData.condition, onClick = { activeLookupField = "CONDITION" })
            }

            Spacer(modifier = Modifier.height(26.dp))
            CalfPrimaryButton(text = "Continue to appearance", onClick = onNextClick)
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TagIdentityScreenPreview() {
    TagIdentityScreen(formData = CalfRegistrationData(), onFormDataChange = {}, onNextClick = {})
}
