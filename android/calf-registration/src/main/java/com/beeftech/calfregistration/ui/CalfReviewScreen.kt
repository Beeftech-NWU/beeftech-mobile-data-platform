package com.beeftech.calfregistration.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalfReviewScreen(
    formData: CalfRegistrationData,
    onJumpToStep: (Int) -> Unit,
    onSaveCalfClick: () -> Unit,
    onBackClick: () -> Unit,
    isSaving: Boolean = false
) {
    Scaffold(
        topBar = {
            CalfStepTopBar(
                step = 4,
                title = "Check",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            BottomActionDock {
                Button(
                    onClick = onSaveCalfClick,
                    enabled = !isSaving && formData.tagNumber.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BeeftechPrimary,
                        contentColor = BeeftechWhite
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = BeeftechWhite,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Saving calf...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save calf",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Review details",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BeeftechText
            )

            // Review row 1: Ear tag
            ReviewRow(
                label = "Ear tag",
                value = formData.tagNumber.ifEmpty { "Not entered" },
                onChangeClick = { onJumpToStep(1) }
            )

            // Review row 2: Type and gender
            ReviewRow(
                label = "Type and gender",
                value = buildString {
                    append(formData.animalType.ifEmpty { "Brangus" })
                    if (formData.gender.isNotBlank()) {
                        append(", ${formData.gender}")
                    }
                },
                onChangeClick = { onJumpToStep(2) }
            )

            // Review row 3: Hide and conformity
            ReviewRow(
                label = "Appearance and score",
                value = buildString {
                    append(formData.hideColour.ifEmpty { "Black" })
                    if (formData.conformity.isNotBlank()) {
                        append(", ${formData.conformity}")
                    }
                    if (formData.mark.isNotBlank()) {
                        append(", mark: ${formData.mark}")
                    }
                },
                onChangeClick = { onJumpToStep(2) }
            )

            ReviewRow(
                label = "Birth details",
                value = buildString {
                    append(
                        formData.birthDate.ifBlank { "Date not entered (today is used)" }
                    )
                    append(", ")
                    append(
                        formData.birthWeightKg.takeIf { it.isNotBlank() }?.let { "$it kg" }
                            ?: "not weighed"
                    )
                },
                onChangeClick = { onJumpToStep(2) }
            )

            // Review row 4: Age and condition
            ReviewRow(
                label = "Age and condition",
                value = buildString {
                    append(formData.age.ifEmpty { "Newborn" })
                    if (formData.condition.isNotBlank()) {
                        append(", ${CalfRegistrationLookups.conditionDisplay(formData.condition)}")
                    }
                },
                onChangeClick = { onJumpToStep(3) }
            )

            // Review row 5: Parents
            val hasDam = com.beeftech.calfregistration.data.CalfRegistrationMappers.parentTag(formData.dameTagNumber) != null
            val hasSire = com.beeftech.calfregistration.data.CalfRegistrationMappers.parentTag(formData.sireTagNumber) != null
            val hasParents = hasDam || hasSire
            ReviewRow(
                label = "Parentage (Dam / Sire)",
                value = if (hasParents) {
                    buildString {
                        if (hasDam) append("Dam: ${formData.dameTagNumber} ")
                        if (hasSire) append("Sire: ${formData.sireTagNumber}")
                    }.trim()
                } else {
                    "None specified"
                },
                onChangeClick = { onJumpToStep(3) }
            )

            // Optional identifiers and proofs: shown only when entered
            if (formData.oldTagNumber.isNotBlank() || formData.referenceNumber.isNotBlank()) {
                ReviewRow(
                    label = "Other identifiers",
                    value = buildString {
                        if (formData.oldTagNumber.isNotBlank()) append("Old tag: ${formData.oldTagNumber} ")
                        if (formData.referenceNumber.isNotBlank()) append("Reference: ${formData.referenceNumber}")
                    }.trim(),
                    onChangeClick = { onJumpToStep(1) }
                )
            }

            if (formData.processProof.isNotBlank() || formData.implantProof.isNotBlank()) {
                ReviewRow(
                    label = "Verification",
                    value = buildString {
                        if (formData.processProof.isNotBlank()) append("Process: ${formData.processProof} ")
                        if (formData.implantProof.isNotBlank()) append("Implant: ${formData.implantProof}")
                    }.trim(),
                    onChangeClick = { onJumpToStep(3) }
                )
            }

            // Review row 6: Photo
            ReviewRow(
                label = "Photo",
                value = if (formData.photoPath != null) "Attached" else "None",
                onChangeClick = { onJumpToStep(3) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Honest offline note card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFEEF3EF),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Saves on this phone. Sends automatically when there is signal.",
                    modifier = Modifier.padding(14.dp),
                    fontSize = 13.5.sp,
                    color = Color(0xFF2B3A31),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
