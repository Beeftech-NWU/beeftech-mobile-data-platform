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
fun AppearanceParentageScreen(
    formData: CalfRegistrationData,
    onFormDataChange: (CalfRegistrationData) -> Unit,
    onBackClick: () -> Unit,
    onDiscardClick: () -> Unit,
    onSaveAndNextClick: () -> Unit
) {
    var activeLookupField by remember { mutableStateOf<String?>(null) }

    // Dialog for lookup selection
    if (activeLookupField != null) {
        val (title, options, currentVal, onSelect) = when (activeLookupField) {
            "HIDE_COLOUR" -> Quadruple(
                "Select Hide Colour",
                CalfRegistrationLookups.hideColours,
                formData.hideColour
            ) { selected: String ->
                onFormDataChange(formData.copy(hideColour = selected))
            }
            "CONFORMITY" -> Quadruple(
                "Select Conformity",
                CalfRegistrationLookups.conformities,
                formData.conformity
            ) { selected: String ->
                onFormDataChange(formData.copy(conformity = selected))
            }
            "DAME" -> Quadruple(
                "Select Dame Tag (F4 List)",
                CalfRegistrationLookups.dameTagList,
                formData.dameTagNumber
            ) { selected: String ->
                onFormDataChange(formData.copy(dameTagNumber = selected))
            }
            else -> Quadruple(
                "Select Sire Tag",
                CalfRegistrationLookups.sireTagList,
                formData.sireTagNumber
            ) { selected: String ->
                onFormDataChange(formData.copy(sireTagNumber = selected))
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
            timeString = "07:22",
            unsyncedCount = 1,
            eyebrow = "REGISTER NEW CALF",
            title = "APPEARANCE & PARENTAGE",
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            // Row 1: HIDE COLOUR & CONFORMITY
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalfLookupDropdownField(
                    label = "HIDE COLOUR",
                    badgeLabel = "LOOKUP",
                    selectedValue = formData.hideColour,
                    onClick = { activeLookupField = "HIDE_COLOUR" },
                    modifier = Modifier.weight(1f)
                )

                CalfLookupDropdownField(
                    label = "CONFORMITY",
                    badgeLabel = "LOOKUP",
                    selectedValue = formData.conformity,
                    onClick = { activeLookupField = "CONFORMITY" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // MARK ("BRAND MERK")
            CalfTextField(
                label = "MARK (\"BRAND MERK\")",
                value = formData.mark,
                onValueChange = { onFormDataChange(formData.copy(mark = it)) },
                placeholder = "—"
            )

            // PARENTAGE SECTION
            CalfSectionDivider(title = "PARENTAGE")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalfLookupDropdownField(
                    label = "DAME TAG NUMBER",
                    badgeLabel = "F4 LIST",
                    selectedValue = formData.dameTagNumber,
                    onClick = { activeLookupField = "DAME" },
                    modifier = Modifier.weight(1f)
                )

                CalfLookupDropdownField(
                    label = "SIRE TAG NUMBER",
                    badgeLabel = "IF AVAILABLE",
                    selectedValue = formData.sireTagNumber,
                    onClick = { activeLookupField = "SIRE" },
                    modifier = Modifier.weight(1f)
                )
            }

            // VERIFICATION SECTION
            CalfSectionDivider(title = "VERIFICATION")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalfTextField(
                    label = "PROCESS PROOF",
                    value = formData.processProof,
                    onValueChange = { onFormDataChange(formData.copy(processProof = it)) },
                    placeholder = "—",
                    modifier = Modifier.weight(1f)
                )

                CalfTextField(
                    label = "IMPLANT PROOF",
                    value = formData.implantProof,
                    onValueChange = { onFormDataChange(formData.copy(implantProof = it)) },
                    placeholder = "—",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // GROUP
            CalfTextField(
                label = "GROUP",
                value = formData.group,
                onValueChange = { onFormDataChange(formData.copy(group = it)) },
                placeholder = "—"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons Row: DISCARD and SAVE & NEXT CALF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalfSecondaryButton(
                    text = "DISCARD",
                    onClick = onDiscardClick,
                    modifier = Modifier.weight(1f)
                )

                CalfPrimaryButton(
                    text = "SAVE & NEXT CALF",
                    onClick = onSaveAndNextClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Design Note
            CalfDesignNoteCard(
                noteText = "Dame and Sire pull from the same F4-list lookup the spec calls out — presented as a searchable picker rather than a free type-in, since tag numbers are easy to mistype in the veld."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppearanceParentageScreenPreview() {
    AppearanceParentageScreen(
        formData = CalfRegistrationData(),
        onFormDataChange = {},
        onBackClick = {},
        onDiscardClick = {},
        onSaveAndNextClick = {}
    )
}


