package com.beeftech.calfregistration.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            CalfSectionTitle("Photo Attachment")
            Spacer(modifier = Modifier.height(12.dp))
            CalfCard {
                if (!formData.photoPath.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BeeftechSoftAccent)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(BeeftechPrimary, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PhotoCamera,
                                    contentDescription = "Photo",
                                    tint = BeeftechWhite,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Photo Attachment Added",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BeeftechText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formData.photoPath.substringAfterLast("/"),
                                    fontSize = 11.sp,
                                    color = BeeftechMutedText
                                )
                            }
                            IconButton(onClick = { onFormDataChange(formData.copy(photoPath = null)) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Remove Photo",
                                    tint = Color(0xFFC62828)
                                )
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            val samplePhotoPath = "/storage/emulated/0/Android/data/com.beeftech/files/photos/calf_${formData.tagNumber}.jpg"
                            onFormDataChange(formData.copy(photoPath = samplePhotoPath))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(11.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BeeftechPrimaryDark)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddAPhoto,
                            contentDescription = "Add Photo",
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Attach Calf Photo (Compressed JPEG)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
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
fun AppearanceParentageScreenPreview() {
    AppearanceParentageScreen(CalfRegistrationData(), {}, {}, {}, {})
}
