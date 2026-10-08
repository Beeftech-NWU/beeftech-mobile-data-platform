package com.beeftech.calfregistration.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.beeftech.calfregistration.util.CalfPhotoCapture
import com.beeftech.tagscanner.ui.EarTagScannerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CalfConditionStepScreen(
    formData: CalfRegistrationData,
    damOptions: List<String>,
    sireOptions: List<String>,
    onFormDataChange: (CalfRegistrationData) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var activeLookupField by remember { mutableStateOf<String?>(null) }
    var scanField by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val latestFormData by rememberUpdatedState(formData)
    val latestOnFormDataChange by rememberUpdatedState(onFormDataChange)

    // Survives the app being recreated while the camera is open.
    var captureFilePath by rememberSaveable { mutableStateOf<String?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val capture = captureFilePath?.let { File(it) }
        captureFilePath = null

        if (taken && capture != null) {
            coroutineScope.launch {
                val saved = withContext(Dispatchers.IO) {
                    CalfPhotoCapture.finalizeCapture(context, capture, latestFormData.tagNumber)
                }
                if (saved != null) {
                    latestOnFormDataChange(latestFormData.copy(photoPath = saved))
                } else {
                    Toast.makeText(context, "Could not read the photo. Please try again.", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            capture?.delete()
        }
    }

    scanField?.let { field ->
        EarTagScannerDialog(
            onDismiss = { scanField = null },
            onTagScanned = { tag ->
                scanField = null
                onFormDataChange(
                    if (field == "DAM") formData.copy(dameTagNumber = tag)
                    else formData.copy(sireTagNumber = tag)
                )
            }
        )
    }

    if (activeLookupField != null) {
        val (title, options, currentVal, onSelect) = when (activeLookupField) {
            "AGE" -> Quadruple(
                "Select Age",
                CalfRegistrationLookups.ages,
                formData.age
            ) { selected: String ->
                onFormDataChange(formData.copy(age = selected))
            }
            "CONDITION" -> Quadruple(
                "Select Condition",
                CalfRegistrationLookups.conditions,
                formData.condition
            ) { selected: String ->
                onFormDataChange(formData.copy(condition = selected))
            }
            "DAM" -> Quadruple(
                "Select Dam Tag",
                listOf(CalfRegistrationLookups.DAME_PLACEHOLDER) + damOptions,
                formData.dameTagNumber
            ) { selected: String ->
                onFormDataChange(formData.copy(dameTagNumber = selected))
            }
            else -> Quadruple(
                "Select Sire Tag",
                listOf(CalfRegistrationLookups.SIRE_PLACEHOLDER) + sireOptions,
                formData.sireTagNumber
            ) { selected: String ->
                onFormDataChange(formData.copy(sireTagNumber = selected))
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
                    if (options.size == 1 && (activeLookupField == "DAM" || activeLookupField == "SIRE")) {
                        Text(
                            text = if (activeLookupField == "DAM") {
                                "No registered females found on this device yet."
                            } else {
                                "No registered males found on this device yet."
                            },
                            color = BeeftechText,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
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
                                    text = "Step 3 of 4: Age & parentage",
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
                                text = "Step 3",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF17402D)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    StepProgress(currentStep = 3, totalSteps = 4)
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
                        text = "Next: check and review",
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
                text = "Age, health and parentage",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BeeftechText
            )

            // Age category
            CalfLookupDropdownField(
                label = "Age Category",
                badgeLabel = "SELECT",
                selectedValue = formData.age.ifEmpty { "Newborn (< 24h)" },
                onClick = { activeLookupField = "AGE" }
            )

            // Condition score
            CalfLookupDropdownField(
                label = "Condition / Vitality",
                badgeLabel = "SELECT",
                selectedValue = formData.condition.ifEmpty { "Good / Alert" },
                onClick = { activeLookupField = "CONDITION" }
            )

            HorizontalDivider(color = BeeftechBorder, thickness = 1.dp)

            Text(
                text = "Parentage (optional)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BeeftechText
            )

            // Dam Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    CalfLookupDropdownField(
                        label = "Dam (Mother) Tag",
                        badgeLabel = "TAG",
                        selectedValue = formData.dameTagNumber.ifEmpty { "None selected" },
                        onClick = { activeLookupField = "DAM" }
                    )
                }
                IconButton(
                    onClick = { scanField = "DAM" },
                    modifier = Modifier
                        .padding(top = 22.dp)
                        .size(48.dp)
                        .background(BeeftechSoftAccent, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DocumentScanner,
                        contentDescription = "Scan Dam Tag",
                        tint = BeeftechPrimary
                    )
                }
            }

            // Sire Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    CalfLookupDropdownField(
                        label = "Sire (Father) Tag",
                        badgeLabel = "TAG",
                        selectedValue = formData.sireTagNumber.ifEmpty { "None selected" },
                        onClick = { activeLookupField = "SIRE" }
                    )
                }
                IconButton(
                    onClick = { scanField = "SIRE" },
                    modifier = Modifier
                        .padding(top = 22.dp)
                        .size(48.dp)
                        .background(BeeftechSoftAccent, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DocumentScanner,
                        contentDescription = "Scan Sire Tag",
                        tint = BeeftechPrimary
                    )
                }
            }

            HorizontalDivider(color = BeeftechBorder, thickness = 1.dp)

            // Verification (optional proof references)
            CalfSectionTitle("Verification (optional)")
            CalfCard {
                CalfTextField(
                    label = "Process proof",
                    value = formData.processProof,
                    onValueChange = { onFormDataChange(formData.copy(processProof = it.take(MAX_PROOF_LENGTH))) },
                    placeholder = "Reference or note"
                )
                Spacer(modifier = Modifier.height(16.dp))
                CalfTextField(
                    label = "Implant proof",
                    value = formData.implantProof,
                    onValueChange = { onFormDataChange(formData.copy(implantProof = it.take(MAX_PROOF_LENGTH))) },
                    placeholder = "Reference or note"
                )
            }

            HorizontalDivider(color = BeeftechBorder, thickness = 1.dp)

            // Photo attachment button: opens the camera, or removes the attached photo
            val hasPhoto = formData.photoPath != null
            OutlinedButton(
                onClick = {
                    if (hasPhoto) {
                        CalfPhotoCapture.deleteSavedPhoto(context, formData.photoPath)
                        onFormDataChange(formData.copy(photoPath = null))
                    } else {
                        try {
                            val (file, uri) = CalfPhotoCapture.newCaptureTarget(context)
                            captureFilePath = file.absolutePath
                            takePicture.launch(uri)
                        } catch (_: Exception) {
                            Toast.makeText(context, "No camera app is available.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (hasPhoto) BeeftechPrimary else BeeftechBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (hasPhoto) BeeftechSoftAccent else BeeftechWhite
                )
            ) {
                Icon(
                    imageVector = if (hasPhoto) Icons.Outlined.Check else Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    tint = if (hasPhoto) BeeftechPrimary else BeeftechMutedText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (hasPhoto) "Calf photo attached (Tap to remove)" else "Take calf photo",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasPhoto) BeeftechPrimary else BeeftechText
                )
            }
        }
    }
}

private const val MAX_PROOF_LENGTH = 200
