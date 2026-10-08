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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeftech.tagscanner.ui.EarTagScannerDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppearanceParentageScreen(
    formData: CalfRegistrationData,
    onFormDataChange: (CalfRegistrationData) -> Unit,
    onBackClick: () -> Unit,
    onDiscardClick: () -> Unit,
    onSaveAndNextClick: () -> Unit
) {
    var activeLookupField by remember { mutableStateOf<String?>(null) }
    // "DAME" or "SIRE" while the ear-tag scanner is open for that field
    var scanField by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (formData.birthDate.isBlank()) {
            val today =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                ).format(Date())

            onFormDataChange(
                formData.copy(
                    birthDate = today
                )
            )
        }
    }

    scanField?.let { field ->
        EarTagScannerDialog(
            onDismiss = { scanField = null },
            onTagScanned = { tag ->
                onFormDataChange(
                    if (field == "DAME") formData.copy(dameTagNumber = tag) else formData.copy(sireTagNumber = tag)
                )
            }
        )
    }

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
            subtitle = "Complete visual and parentage details.",
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
            CalfSectionTitle("Birth Details")
            Spacer(modifier = Modifier.height(12.dp))
            CalfCard {
                CalfDatePickerField(
                    label = "Birth date",
                    value = formData.birthDate,
                    onValueChange = { selectedDate ->
                        onFormDataChange(
                            formData.copy(
                                birthDate = selectedDate
                            )
                        )
                    },
                    supportingText = "Tap the calendar to select the calf's actual birth date."
                )

                Spacer(modifier = Modifier.height(16.dp))

                CalfTextField(
                    label = "Birth mass (kg)",
                    value = formData.birthWeightKg,
                    onValueChange = { rawValue ->
                        val normalized =
                            rawValue
                                .replace(",", ".")
                                .filterIndexed { index, character ->
                                    character.isDigit() ||
                                        (
                                            character == '.' &&
                                                index > 0
                                        )
                                }
                                .let { candidate ->
                                    val firstDot =
                                        candidate.indexOf('.')

                                    if (firstDot < 0) {
                                        candidate
                                    } else {
                                        candidate
                                            .substring(
                                                0,
                                                firstDot + 1
                                            ) +
                                            candidate
                                                .substring(
                                                    firstDot + 1
                                                )
                                                .replace(
                                                    ".",
                                                    ""
                                                )
                                    }
                                }

                        onFormDataChange(
                            formData.copy(
                                birthWeightKg = normalized
                            )
                        )
                    },
                    placeholder = "e.g. 35.0",
                    supportingText = "Enter the measured mass when available. If the calf was not weighed, choose an estimate below.",
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Decimal
                        )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "QUICK ESTIMATE — USE ONLY WHEN NOT WEIGHED",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = BeeftechPrimaryDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "30" to "Small",
                        "35" to "Typical",
                        "40" to "Large"
                    ).forEach { (mass, label) ->
                        OutlinedButton(
                            onClick = {
                                onFormDataChange(
                                    formData.copy(
                                        birthWeightKg = mass
                                    )
                                )
                            },
                            modifier =
                                Modifier.weight(1f),
                            contentPadding =
                                PaddingValues(
                                    horizontal = 6.dp,
                                    vertical = 9.dp
                                )
                        ) {
                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$mass kg",
                                    fontWeight =
                                        FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = label,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Estimated mass is a fallback only. Replace it with a measured weight when possible.",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = BeeftechMutedText
                )
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
                Spacer(modifier = Modifier.height(8.dp))
                CalfSecondaryButton(text = "Scan dam tag", onClick = { scanField = "DAME" })
                Spacer(modifier = Modifier.height(16.dp))
                CalfLookupDropdownField("Sire tag number", "If available", formData.sireTagNumber, { activeLookupField = "SIRE" })
                Spacer(modifier = Modifier.height(8.dp))
                CalfSecondaryButton(text = "Scan sire tag", onClick = { scanField = "SIRE" })
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
