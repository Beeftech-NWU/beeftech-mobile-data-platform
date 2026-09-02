package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CalfRegistration
import com.example.data.model.UserSession
import com.example.ui.components.LivestockEarTagVisual
import com.example.ui.theme.*
import com.example.ui.viewmodel.RegistrationFormState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalfRegistrationScreen(
    formState: RegistrationFormState,
    userSession: UserSession,
    knownDamIds: List<String>,
    knownSireIds: List<String>,
    onAnimalIdChanged: (String) -> Unit,
    onBirthDateChanged: (String) -> Unit,
    onBreedChanged: (String) -> Unit,
    onSexChanged: (String) -> Unit,
    onBirthWeightChanged: (Double?) -> Unit,
    onCalvingEaseChanged: (Int) -> Unit,
    onVigorChanged: (String) -> Unit,
    onHornStatusChanged: (String) -> Unit,
    onPastureLocationChanged: (String) -> Unit,
    onRfidTagChanged: (String) -> Unit,
    onGenerateMockRfid: () -> Unit,
    onDamIdChanged: (String) -> Unit,
    onSireIdChanged: (String) -> Unit,
    onPhotoSelected: (String?) -> Unit,
    onSubmit: () -> Unit,
    onClearSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToRegistry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isBreedDropdownExpanded by remember { mutableStateOf(false) }
    var isPastureDropdownExpanded by remember { mutableStateOf(false) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }

    // Date Picker Dialog setup
    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onBirthDateChanged(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }
    }

    // Success Confirmation Dialog
    if (formState.submissionSuccess != null) {
        val saved = formState.submissionSuccess
        AlertDialog(
            onDismissRequest = { onClearSuccess() },
            icon = {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD1FAE5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(36.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Calf Registered Successfully",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LivestockEarTagVisual(
                        tagNumber = saved.animalId,
                        breed = saved.breed,
                        sex = saved.sex,
                        rfidTag = saved.rfidTag,
                        isLarge = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Tag: ${saved.animalId} (${saved.sex})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${saved.breed} • ${saved.birthWeightKg ?: "-"} kg • ${saved.pastureLocation}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Audit Record GUID:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = saved.recordGuid,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Stored locally in SQLite with status PENDING.",
                        fontSize = 12.sp,
                        color = Color(0xFFB45309),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onClearSuccess() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("register_another_button")
                ) {
                    Text("Register Another Calf")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onClearSuccess()
                        onNavigateToRegistry()
                    },
                    modifier = Modifier.testTag("view_in_registry_button")
                ) {
                    Text("View in Registry")
                }
            }
        )
    }

    // Photo selection dialog
    if (showPhotoOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoOptionsDialog = false },
            title = { Text("Attach Field / Tag Photo", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Capture ear tag stamped visual or side-profile calf image for biometrics archive:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            onPhotoSelected("res://drawable/beeftech_banner")
                            showPhotoOptionsDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simulate Camera Capture")
                    }
                    if (formState.photoPath != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                onPhotoSelected(null)
                                showPhotoOptionsDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Remove Photo")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoOptionsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = "Register New Calf",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Rugged Field Capture & Electronic Tagging",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Duplicate Error Banner
        if (formState.isDuplicateId && formState.duplicateErrorMessage != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("duplicate_id_warning_banner"),
                    colors = CardDefaults.cardColors(containerColor = StatusErrorBg),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, StatusErrorBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Duplicate Warning",
                            tint = StatusErrorText,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DUPLICATE ANIMAL ID DETECTED",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = StatusErrorText
                            )
                            Text(
                                text = formState.duplicateErrorMessage,
                                fontSize = 12.sp,
                                color = StatusErrorText
                            )
                        }
                    }
                }
            }
        }

        // Interactive Live Ear Tag Preview
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Loyalty,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE EAR TAG PREVIEW",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Physical livestock identification badge stamped according to national traceability standards.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (formState.rfidTag.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "RFID: ${formState.rfidTag}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0369A1)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    LivestockEarTagVisual(
                        tagNumber = formState.animalId.ifBlank { "ZA-0000" },
                        breed = formState.breed,
                        sex = formState.sex,
                        rfidTag = formState.rfidTag,
                        isLarge = true
                    )
                }
            }
        }

        // Section 1: ANIMAL IDENTIFICATION & RFID TAG
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Animal Tag & RFID *",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (formState.isCheckingDuplicate) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else if (formState.animalId.isNotBlank() && !formState.isDuplicateId) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Unique ID",
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ID Available",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Animal ID Input Field
                    OutlinedTextField(
                        value = formState.animalId,
                        onValueChange = onAnimalIdChanged,
                        label = { Text("Ear Tag Number (Visual ID) *") },
                        placeholder = { Text("e.g. ZA-26-0901") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Numbers, contentDescription = "ID")
                        },
                        trailingIcon = {
                            if (formState.animalId.isNotEmpty()) {
                                IconButton(onClick = { onAnimalIdChanged("") }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        isError = formState.isDuplicateId,
                        supportingText = {
                            if (formState.isDuplicateId) {
                                Text(
                                    text = "This ID already exists in database",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text("Physical ear tag number stamped on animal")
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("animal_id_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Prefix Shortcuts
                    Text(
                        text = "Quick ear-tag prefix presets:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("ZA-26-", "BT-2026-", "CALF-").forEach { prefix ->
                            OutlinedButton(
                                onClick = {
                                    if (!formState.animalId.startsWith(prefix)) {
                                        onAnimalIdChanged(prefix)
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = "+ $prefix", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Electronic RFID / EID field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = formState.rfidTag,
                            onValueChange = onRfidTagChanged,
                            label = { Text("Electronic RFID Tag (ISO 11784)") },
                            placeholder = { Text("e.g. 982 000182938471") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Nfc, contentDescription = "RFID")
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rfid_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onGenerateMockRfid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                            modifier = Modifier.testTag("scan_rfid_button")
                        ) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scan")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan RFID", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section 2: CALF BIOMETRICS (Sex, Birth Weight, Calving Ease, Vigor)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonitorWeight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Calf Biometrics & Vigor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sex Selector
                    Text(
                        text = "Sex / Gender *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            CalfRegistration.SEX_HEIFER to Icons.Default.Female,
                            CalfRegistration.SEX_BULL to Icons.Default.Male,
                            CalfRegistration.SEX_STEER to null
                        ).forEach { (sexOption, icon) ->
                            val isSelected = formState.sex.equals(sexOption, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSexChanged(sexOption) },
                                leadingIcon = if (icon != null) {
                                    { Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                label = {
                                    Text(
                                        text = sexOption,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (sexOption == CalfRegistration.SEX_HEIFER) SexHeiferBg else SexBullBg,
                                    selectedLabelColor = if (sexOption == CalfRegistration.SEX_HEIFER) SexHeiferText else SexBullText,
                                    selectedLeadingIconColor = if (sexOption == CalfRegistration.SEX_HEIFER) SexHeiferText else SexBullText
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sex_chip_${sexOption.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Birth Weight Stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Birth Weight (kg)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Standard newborn target: 30–42 kg",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Stepper Controls
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalIconButton(
                                onClick = {
                                    val current = formState.birthWeightKg ?: 34.0
                                    if (current > 15.0) onBirthWeightChanged(current - 0.5)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease weight")
                            }

                            Text(
                                text = "${formState.birthWeightKg ?: "--"} kg",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            FilledTonalIconButton(
                                onClick = {
                                    val current = formState.birthWeightKg ?: 34.0
                                    if (current < 65.0) onBirthWeightChanged(current + 0.5)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase weight")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick weight presets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(30.0, 34.0, 38.0, 42.0).forEach { weightPreset ->
                            OutlinedButton(
                                onClick = { onBirthWeightChanged(weightPreset) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("${weightPreset.toInt()} kg", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Calving Ease Score (1 to 4)
                    Text(
                        text = "Calving Ease Score",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            CalfRegistration.CALVING_EASE_UNASSISTED to "1: Normal",
                            CalfRegistration.CALVING_EASE_EASY_PULL to "2: Easy Pull",
                            CalfRegistration.CALVING_EASE_HARD_PULL to "3: Hard Pull",
                            CalfRegistration.CALVING_EASE_SURGICAL to "4: Vet/Surg"
                        ).forEach { (score, label) ->
                            val isSelected = formState.calvingEase == score
                            FilterChip(
                                selected = isSelected,
                                onClick = { onCalvingEaseChanged(score) },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Vigor & Horn Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Vigor
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Calf Vigor",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            listOf(
                                CalfRegistration.VIGOR_VIGOROUS,
                                CalfRegistration.VIGOR_MODERATE,
                                CalfRegistration.VIGOR_WEAK
                            ).forEach { vigorOption ->
                                val isSelected = formState.vigor == vigorOption
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onVigorChanged(vigorOption) },
                                    label = { Text(vigorOption.split("/").first().trim(), fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Horn status
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Horn Status",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            listOf(
                                CalfRegistration.HORN_POLLED,
                                CalfRegistration.HORN_HORNED,
                                CalfRegistration.HORN_DEHORNED
                            ).forEach { hornOption ->
                                val isSelected = formState.hornStatus == hornOption
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onHornStatusChanged(hornOption) },
                                    label = { Text(hornOption.split("(").first().trim(), fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: DATE OF BIRTH, BREED & PASTURE LOCATION
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Birth Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Date of Birth *",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Quick buttons: Today / Yesterday
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    onBirthDateChanged(format.format(Date()))
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Today", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    val yesterday = Date(System.currentTimeMillis() - 86400000L)
                                    onBirthDateChanged(format.format(yesterday))
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Yesterday", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = formState.birthDate,
                        onValueChange = onBirthDateChanged,
                        label = { Text("Birth Date (YYYY-MM-DD) *") },
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Pick date")
                            }
                        },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerDialog.show() }
                            .testTag("birth_date_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Breed Selector
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Calf Breed *",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dropdown for Breeds
                    ExposedDropdownMenuBox(
                        expanded = isBreedDropdownExpanded,
                        onExpandedChange = { isBreedDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = formState.breed,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Breed *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isBreedDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("breed_selector")
                        )

                        ExposedDropdownMenu(
                            expanded = isBreedDropdownExpanded,
                            onDismissRequest = { isBreedDropdownExpanded = false }
                        ) {
                            CalfRegistration.STANDARD_BREEDS.forEach { breedName ->
                                DropdownMenuItem(
                                    text = { Text(breedName, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        onBreedChanged(breedName)
                                        isBreedDropdownExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Popular South African & Global beef breed presets
                    Text(
                        text = "South African & Global Breeds (1-tap):",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Bonsmara", "Brahman", "Nguni", "Angus", "Simmentaler", "Afrikaner", "Drakensberger", "Boran").forEach { breedOption ->
                            val isSelected = formState.breed.equals(breedOption, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onBreedChanged(breedOption) },
                                label = { Text(breedOption, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pasture / Camp Location Selector
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Landscape,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ranch Camp / Pasture Location",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = isPastureDropdownExpanded,
                        onExpandedChange = { isPastureDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = formState.pastureLocation,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Paddock / Kraal Assignment") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPastureDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = isPastureDropdownExpanded,
                            onDismissRequest = { isPastureDropdownExpanded = false }
                        ) {
                            CalfRegistration.PASTURE_LOCATIONS.forEach { pastureName ->
                                DropdownMenuItem(
                                    text = { Text(pastureName, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        onPastureLocationChanged(pastureName)
                                        isPastureDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 4: PARENTAGE (Dam / Sire ID)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Parentage / Lineage (Optional)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Capture mother (dam) and father (sire) IDs for pedigree traceability.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dam ID
                    OutlinedTextField(
                        value = formState.damId,
                        onValueChange = onDamIdChanged,
                        label = { Text("Dam ID (Mother Ear Tag)") },
                        placeholder = { Text("e.g. DAM-BN-440") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dam_id_input")
                    )

                    if (knownDamIds.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Recent Dams:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            knownDamIds.take(2).forEach { existingDam ->
                                TextButton(
                                    onClick = { onDamIdChanged(existingDam) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(existingDam, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sire ID
                    OutlinedTextField(
                        value = formState.sireId,
                        onValueChange = onSireIdChanged,
                        label = { Text("Sire ID (Father / Stud Bull Tag)") },
                        placeholder = { Text("e.g. BULL-BN-902") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sire_id_input")
                    )

                    if (knownSireIds.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Stud Bulls:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            knownSireIds.take(2).forEach { existingSire ->
                                TextButton(
                                    onClick = { onSireIdChanged(existingSire) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(existingSire, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 5: PHOTO / MEDIA CAPTURE
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Visual Record & Tag Photo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "OPTIONAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (formState.photoPath != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.beeftech_banner),
                                    contentDescription = "Calf Photo Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Photo Attached",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Field visual record captured",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { onPhotoSelected(null) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove photo",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showPhotoOptionsDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("attach_photo_button")
                        ) {
                            Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Attach Calf Photo / Ear Tag Image")
                        }
                    }
                }
            }
        }

        // Section 6: IMMUTABLE AUDIT METADATA PREVIEW
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Immutable Audit Metadata (Auto-Captured)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "READ-ONLY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("GPS Coordinates:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${formState.mockGpsLat}, ${formState.mockGpsLng}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Field Terminal Device ID:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(userSession.deviceId, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text("Generated GUID:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formState.generatedGuidPreview,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Section 7: SUBMIT BUTTON
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onSubmit,
                    enabled = formState.isFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("save_calf_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (formState.isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("SAVING TO LOCAL SQLITE...", fontWeight = FontWeight.Black, fontSize = 15.sp)
                    } else {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SAVE CALF REGISTRATION", fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }

                if (!formState.isFormValid && !formState.isSubmitting) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            formState.animalId.isBlank() -> "• Enter Animal ID to enable saving"
                            formState.isDuplicateId -> "• Change duplicate Animal ID"
                            formState.birthDate.isBlank() -> "• Select Birth Date"
                            formState.breed.isBlank() -> "• Select Breed"
                            else -> ""
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
