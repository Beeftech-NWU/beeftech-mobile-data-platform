package com.beeftech.calfregistration.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AppearanceParentageScreen(
    formData: CalfRegistrationData,
    onFormDataChange: (CalfRegistrationData) -> Unit,
    onBackClick: () -> Unit,
    onDiscardClick: () -> Unit,
    onSaveAndNextClick: () -> Unit
) {
    var activeLookupField by remember { mutableStateOf<String?>(null) }

    if (activeLookupField != null) {
        val (title, options, currentVal, onSelect) = when (activeLookupField) {
            "HIDE_COLOUR" -> Quadruple("Select Hide Colour", CalfRegistrationLookups.hideColours, formData.hideColour) { selected: String ->
                onFormDataChange(formData.copy(hideColour = selected))
            }
            "CONFORMITY" -> Quadruple("Select Conformity", CalfRegistrationLookups.conformities, formData.conformity) { selected: String ->
                onFormDataChange(formData.copy(conformity = selected))
            }
            "DAME" -> Quadruple("Select Dam Tag", CalfRegistrationLookups.dameTagList, formData.dameTagNumber) { selected: String ->
                onFormDataChange(formData.copy(dameTagNumber = selected))
            }
            else -> Quadruple("Select Sire Tag", CalfRegistrationLookups.sireTagList, formData.sireTagNumber) { selected: String ->
                onFormDataChange(formData.copy(sireTagNumber = selected))
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
            title = "Appearance & Parentage",
            subtitle = "Complete visual, parentage and verification details.",
            icon = Icons.Outlined.AccountTree,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            CalfSectionTitle("Appearance")
            Spacer(modifier = Modifier.height(12.dp))
            CalfCard {
                CalfLookupDropdownField("Hide colour", selectedValue = formData.hideColour, onClick = { activeLookupField = "HIDE_COLOUR" })
                Spacer(modifier = Modifier.height(16.dp))
                CalfLookupDropdownField("Conformity", selectedValue = formData.conformity, onClick = { activeLookupField = "CONFORMITY" })
                Spacer(modifier = Modifier.height(16.dp))
                CalfTextField("Mark (brand merk)", formData.mark, { onFormDataChange(formData.copy(mark = it)) })
            }

            Spacer(modifier = Modifier.height(24.dp))
            CalfSectionTitle("Parentage")
            Spacer(modifier = Modifier.height(12.dp))
            CalfCard {
                CalfLookupDropdownField("Dam tag number", "F4 list", formData.dameTagNumber, { activeLookupField = "DAME" })
                Spacer(modifier = Modifier.height(16.dp))
                CalfLookupDropdownField("Sire tag number", "If available", formData.sireTagNumber, { activeLookupField = "SIRE" })
            }

            Spacer(modifier = Modifier.height(24.dp))
            CalfSectionTitle("Verification")
            Spacer(modifier = Modifier.height(12.dp))
            CalfCard {
                CalfTextField("Process proof", formData.processProof, { onFormDataChange(formData.copy(processProof = it)) })
                Spacer(modifier = Modifier.height(16.dp))
                CalfTextField("Implant proof", formData.implantProof, { onFormDataChange(formData.copy(implantProof = it)) })
                Spacer(modifier = Modifier.height(16.dp))
                CalfTextField("Group", formData.group, { onFormDataChange(formData.copy(group = it)) })
            }

            Spacer(modifier = Modifier.height(26.dp))
            CalfSecondaryButton(text = "Discard registration", onClick = onDiscardClick)
            Spacer(modifier = Modifier.height(12.dp))
            CalfPrimaryButton(text = "Save and register next calf", onClick = onSaveAndNextClick)
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppearanceParentageScreenPreview() {
    AppearanceParentageScreen(CalfRegistrationData(), {}, {}, {}, {})
}
