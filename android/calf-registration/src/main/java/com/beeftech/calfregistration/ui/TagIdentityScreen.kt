package com.beeftech.calfregistration.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeftech.calfregistration.util.TagColour
import com.beeftech.calfregistration.util.TagNamingUtils

@Composable
fun TagIdentityScreen(
    formData: CalfRegistrationData,
    onFormDataChange: (CalfRegistrationData) -> Unit,
    onNextClick: () -> Unit
) {
    var activeLookupField by remember { mutableStateOf<String?>(null) }

    val extractedComponents = remember(formData.tagNumber) {
        TagNamingUtils.extractComponents(formData.tagNumber)
    }
    val currentColour = extractedComponents?.first ?: TagColour.BLUE
    val currentSequence = extractedComponents?.second ?: ""
    val isTagValid = TagNamingUtils.validateTag(formData.tagNumber)

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
            CalfSectionTitle("Ear Tag Naming Standard")
            Spacer(modifier = Modifier.height(12.dp))
            CalfCard {
                Text(
                    text = "TAG COLOUR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = BeeftechPrimaryDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TagColour.entries.forEach { colour ->
                        val isSelected = currentColour == colour
                        val chipBgColor = when (colour) {
                            TagColour.BLUE -> Color(0xFFE3F2FD)
                            TagColour.RED -> Color(0xFFFFEBEE)
                            TagColour.GREEN -> Color(0xFFE8F5E9)
                            TagColour.YELLOW -> Color(0xFFFFFDE7)
                        }
                        val chipTextColor = when (colour) {
                            TagColour.BLUE -> Color(0xFF0D47A1)
                            TagColour.RED -> Color(0xFFB71C1C)
                            TagColour.GREEN -> Color(0xFF1B5E20)
                            TagColour.YELLOW -> Color(0xFFF57F17)
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val seqNum = currentSequence.toLongOrNull() ?: 64L
                                    val updatedTag = TagNamingUtils.formatTag(colour, seqNum)
                                    onFormDataChange(formData.copy(tagNumber = updatedTag))
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) chipBgColor else BeeftechWhite,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) chipTextColor else BeeftechBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(chipTextColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = colour.prefix,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) chipTextColor else BeeftechText
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                CalfTextField(
                    label = "Tag sequence or shorthand (e.g. 64 or B64)",
                    value = formData.tagNumber,
                    onValueChange = { input ->
                        val expanded = TagNamingUtils.parseAndExpand(input)
                        onFormDataChange(formData.copy(tagNumber = expanded))
                    },
                    placeholder = "e.g. 64 or B64 or Blu0000064",
                    supportingText = "Accepts quick search codes like B64, R123, G45, Y78",
                    icon = Icons.Outlined.Tag
                )

                Spacer(modifier = Modifier.height(14.dp))

                val badgeBgColor = when (currentColour) {
                    TagColour.BLUE -> Color(0xFF1565C0)
                    TagColour.RED -> Color(0xFFC62828)
                    TagColour.GREEN -> Color(0xFF2E7D32)
                    TagColour.YELLOW -> Color(0xFFF57F17)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = badgeBgColor.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, badgeBgColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeBgColor
                        ) {
                            Text(
                                text = currentColour.prefix.uppercase(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Standard Ear Tag ID",
                                fontSize = 10.sp,
                                color = BeeftechMutedText,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formData.tagNumber,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = BeeftechText
                            )
                        }
                        if (isTagValid) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Valid Tag",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = "Invalid Tag",
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

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
