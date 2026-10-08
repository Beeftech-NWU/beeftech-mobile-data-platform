package com.beeftech.farmtraceability.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Female
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.beeftech.farmtraceability.repository.AnimalMassReading
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AnimalRecordScreen(
    tagNumber: String = "",
    breed: String = "",
    gender: String = "",
    photoPath: String = "",
    birthDate: String = "",
    age: String = "",
    currentLocation: String = "",
    supplierName: String = "",
    status: String = "",
    entryMass: String = "",
    registeredBirthMass: String = "",
    lastMass: String = "",
    daysAtFacility: String = "",
    averageDailyGain: String = "",
    hasCalfRegistration: Boolean = false,
    massHistory: List<AnimalMassReading> = emptyList(),
    movementCount: Int = 0,
    feedCount: Int = 0,
    treatmentCount: Int = 0,
    mortalityCount: Int = 0,
    totalCost: String = "",
    onBackClick: () -> Unit = {},
    onSupplierClick: () -> Unit = {},
    onLocationFeedClick: () -> Unit = {},
    onTreatmentsClick: () -> Unit = {},
    onAnimalMovementClick: () -> Unit = {},
    onCostSummaryClick: () -> Unit = {},
    onSaveRegisteredMass: (String, (Boolean, String) -> Unit) -> Unit = { _, done ->
        done(false, "Saving registered mass is unavailable.")
    },
    onSaveWeighing: (String, String, String, (Boolean, String) -> Unit) -> Unit = { _, _, _, done ->
        done(false, "Saving a weighing is unavailable.")
    },
    onMortalityClick: () -> Unit = {}
) {
    var showMassEditor by remember(tagNumber) { mutableStateOf(false) }
    var editingRegistered by remember(tagNumber) { mutableStateOf(true) }
    var massInput by remember(tagNumber) { mutableStateOf("") }
    var massDate by remember(tagNumber) {
        mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
    }
    var massNote by remember(tagNumber) { mutableStateOf("") }
    var massError by remember(tagNumber) { mutableStateOf("") }
    var massSaving by remember(tagNumber) { mutableStateOf(false) }

    fun openMassEditor() {
        editingRegistered = hasCalfRegistration
        massInput = if (hasCalfRegistration) registeredBirthMass.removeSuffix("kg").trim() else ""
        massDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        massNote = ""
        massError = ""
        showMassEditor = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeeftechBackground)
            .verticalScroll(rememberScrollState())
    ) {
        TraceabilityHeader(
            eyebrow = "BEEFTECH",
            title = "Animal Record",
            subtitle = "Profile and linked traceability records",
            icon = Icons.Outlined.Route,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimalRecordProfileCard(
                tagNumber = tagNumber,
                breed = breed,
                gender = gender,
                photoPath = photoPath,
                age = age,
                lastMass = lastMass,
                status = status
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimalInfoTile(
                    icon = Icons.Outlined.CalendarMonth,
                    label = "Birth Date",
                    value = birthDate.ifBlank { "Unavailable" },
                    modifier = Modifier.weight(1f)
                )
                AnimalInfoTile(
                    icon = Icons.Outlined.Timer,
                    label = "Age",
                    value = age.ifBlank { "Unavailable" },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimalInfoTile(
                    icon = Icons.Outlined.LocationOn,
                    label = "Current Location",
                    value =
                        currentLocation.ifBlank {
                            "Add location"
                        },
                    modifier = Modifier.weight(1f),
                    onClick = onLocationFeedClick
                )
                AnimalInfoTile(
                    icon = Icons.Outlined.MonitorWeight,
                    label = "Registered Mass",
                    value = registeredBirthMass.ifBlank { "Tap to add mass" },
                    modifier = Modifier.weight(1f),
                    onClick = { openMassEditor() }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimalInfoTile(
                    icon = Icons.Outlined.LocalShipping,
                    label = "Supplier",
                    value = supplierName.ifBlank { "Not recorded" },
                    modifier = Modifier.weight(1f),
                    onClick = onSupplierClick
                )
                AnimalInfoTile(
                    icon = Icons.Outlined.Female,
                    label = "Sex / Breed",
                    value = listOf(breed, gender).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Unavailable" },
                    modifier = Modifier.weight(1f)
                )
            }

            if (entryMass.isNotBlank() || daysAtFacility.isNotBlank() || averageDailyGain.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BeeftechSoftAccent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PerformanceValue("Entry", entryMass.ifBlank { "—" })
                        PerformanceValue("Days", daysAtFacility.ifBlank { "—" })
                        PerformanceValue(
                            "ADG",
                            averageDailyGain.ifBlank { "Not enough data" },
                            "Average Daily Gain is the change between two recorded masses divided by the calendar days between those measurements."
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            TraceabilitySectionTitle("Linked Records")

            RecordLinkCard(
                title = "Movement History",
                subtitle = "$movementCount movement${if (movementCount == 1) "" else "s"}",
                icon = Icons.Outlined.Route,
                accent = Color(0xFF1B8C55),
                onClick = onAnimalMovementClick
            )

            RecordLinkCard(
                title = "Feed Records",
                subtitle = "$feedCount feed reading${if (feedCount == 1) "" else "s"}",
                icon = Icons.Outlined.Spa,
                accent = Color(0xFF16915E),
                onClick = onLocationFeedClick
            )

            RecordLinkCard(
                title = "Treatments",
                subtitle = "$treatmentCount treatment${if (treatmentCount == 1) "" else "s"}",
                icon = Icons.Outlined.Medication,
                accent = Color(0xFF2668A9),
                onClick = onTreatmentsClick
            )

            RecordLinkCard(
                title = "Mortalities",
                subtitle = if (mortalityCount == 0) "No mortality records" else "$mortalityCount mortality record${if (mortalityCount == 1) "" else "s"}",
                icon = Icons.AutoMirrored.Outlined.Assignment,
                accent = Color(0xFFD94B4B),
                onClick = onMortalityClick
            )

            RecordLinkCard(
                title = "Costs",
                subtitle =
                    totalCost.ifBlank {
                        "No recorded costs"
                    },
                icon = Icons.Outlined.Payments,
                accent = Color(0xFF1B7A52),
                onClick = onCostSummaryClick
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showMassEditor) {
        AlertDialog(
            onDismissRequest = { if (!massSaving) showMassEditor = false },
            title = { Text("Mass records") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text("Update mass without leaving Animal Record.", fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            enabled = !massSaving && hasCalfRegistration,
                            onClick = {
                                editingRegistered = true
                                massInput = registeredBirthMass.removeSuffix("kg").trim()
                                massError = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Correct birth mass", fontSize = 10.sp) }
                        OutlinedButton(
                            enabled = !massSaving,
                            onClick = {
                                editingRegistered = false
                                massInput = ""
                                massError = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("New weighing", fontSize = 10.sp) }
                    }
                    if (editingRegistered) {
                        Text(
                            "This updates the original Calf Registration mass; no extra mass record is created.",
                            fontSize = 11.sp,
                            color = BeeftechMutedText
                        )
                    }
                    TraceabilityTextField(
                        label = if (editingRegistered) "Registered Mass (kg)" else "Weighing Mass (kg)",
                        value = massInput,
                        onValueChange = { massInput = it; massError = "" },
                        icon = Icons.Outlined.MonitorWeight,
                        placeholder = "e.g. 218.5",
                        required = true,
                        numeric = true,
                        decimal = true
                    )
                    if (!editingRegistered) {
                        TraceabilityDatePickerField(
                            label = "Weighing Date",
                            value = massDate,
                            onValueChange = { massDate = it; massError = "" },
                            required = true,
                            maxToday = true
                        )
                        TraceabilityTextField(
                            label = "Weighing Note",
                            value = massNote,
                            onValueChange = { massNote = it },
                            icon = Icons.Outlined.MonitorWeight,
                            placeholder = "Optional note"
                        )
                    }
                    if (massHistory.isNotEmpty()) {
                        Text("Mass history", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        massHistory.take(6).forEach { reading ->
                            val dateLabel = SimpleDateFormat(
                                "dd MMM yyyy", Locale.getDefault()
                            ).format(Date(reading.dateMillis))
                            val massLabel = "%.1f".format(Locale.US, reading.massKg)
                            Text(
                                "$dateLabel · $massLabel kg · ${reading.source}",
                                fontSize = 11.sp,
                                color = BeeftechMutedText
                            )
                        }
                    }
                    if (massError.isNotBlank()) {
                        Text(massError, color = Color(0xFFB13E3A), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !massSaving,
                    onClick = {
                        val parsed = massInput.replace(",", ".").trim().toDoubleOrNull()
                        if (parsed == null || !parsed.isFinite() || parsed <= 0.0) {
                            massError = "Enter a valid mass greater than zero."
                        } else {
                            massSaving = true
                            val done: (Boolean, String) -> Unit = { success, message ->
                                massSaving = false
                                if (success) showMassEditor = false
                                else massError = message
                            }
                            if (editingRegistered) onSaveRegisteredMass(massInput, done)
                            else onSaveWeighing(massInput, massDate, massNote, done)
                        }
                    }
                ) { Text(if (massSaving) "Saving…" else "Save") }
            },
            dismissButton = {
                TextButton(onClick = { showMassEditor = false }, enabled = !massSaving) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AnimalRecordProfileCard(
    tagNumber: String,
    breed: String,
    gender: String,
    photoPath: String,
    age: String,
    lastMass: String,
    status: String
) {
    val bitmap = remember(photoPath) {
        photoPath.takeIf { it.isNotBlank() }
            ?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
            ?.asImageBitmap()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BeeftechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(BeeftechSoftAccent),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Animal photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("🐄", fontSize = 46.sp)
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tagNumber.ifBlank { "No animal selected" },
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = BeeftechText,
                        modifier = Modifier.weight(1f)
                    )
                    AnimalRecordStatus(status)
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = listOf(breed, gender).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Breed unavailable" },
                    fontSize = 12.sp,
                    color = BeeftechMutedText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOf(age, lastMass).filter { it.isNotBlank() }.joinToString("  ·  "),
                    fontSize = 12.sp,
                    color = BeeftechMutedText
                )
            }
        }
    }
}

@Composable
private fun AnimalRecordStatus(status: String) {
    val deceased = status.equals("Deceased", true)
    val sold = status.equals("Sold", true)
    val moved = status.equals("Moved", true)
    val knownStatus = status.isNotBlank()

    val background = when {
        deceased -> Color(0xFFFFE1DF)
        sold -> Color(0xFFFFEDD4)
        moved -> Color(0xFFDCEEFF)
        knownStatus -> Color(0xFFDDF4E4)
        else -> Color(0xFFF1F3F2)
    }

    val foreground = when {
        deceased -> Color(0xFFB13E3A)
        sold -> Color(0xFF9A6517)
        moved -> Color(0xFF2C6CA3)
        knownStatus -> Color(0xFF2D774C)
        else -> BeeftechMutedText
    }

    Surface(
        shape = RoundedCornerShape(50.dp),
        color = background
    ) {
        Text(
            text =
                status.ifBlank {
                    "Status unavailable"
                },
            modifier =
                Modifier.padding(
                    horizontal = 9.dp,
                    vertical = 5.dp
                ),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = foreground
        )
    }
}

@Composable
private fun AnimalInfoTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BeeftechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BeeftechPrimaryDark,
                modifier = Modifier.size(21.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 10.sp, color = BeeftechMutedText)
            Text(
                value,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = BeeftechText,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun PerformanceValue(label: String, value: String, help: String = "") {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (help.isNotBlank()) {
            TraceabilityHelpLabel(label, help)
        } else {
            Text(label, fontSize = 10.sp, color = BeeftechMutedText)
        }
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BeeftechPrimaryDeep)
    }
}

@Composable
private fun RecordLinkCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BeeftechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(10.dp).size(22.dp)
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BeeftechText)
                Text(subtitle, fontSize = 11.sp, color = BeeftechMutedText)
            }
            Text("›", fontSize = 26.sp, color = BeeftechPrimaryDark)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AnimalRecordScreenPreview() {
    AnimalRecordScreen()
}
