package com.beeftech.farmtraceability.ui

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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.beeftech.database.entity.AnimalMovementEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class MovementTypeOption(
    val label: String,
    val icon: ImageVector
)

private val MovementTypes = listOf(
    MovementTypeOption("Within Site", Icons.Outlined.Home),
    MovementTypeOption("Another Site", Icons.Outlined.LocalShipping),
    MovementTypeOption("Sold", Icons.Outlined.Sell),
    MovementTypeOption("Other", Icons.Outlined.MoreHoriz)
)

@Composable
fun AnimalMovementScreen(
    animalReference: String = "",
    movementInformation: String = "",
    responsibleWorker: String = "",
    workerOptions: List<String> = emptyList(),
    foundAnimalReference: String = "",
    foundMovementInformation: String = "",
    foundMovementDate: String = "",
    foundResponsibleWorker: String = "",
    movementRecords: List<AnimalMovementEntity> = emptyList(),
    onBackClick: () -> Unit = {},
    onAnimalReferenceChange: (String) -> Unit = {},
    onMovementInformationChange: (String) -> Unit = {},
    onResponsibleWorkerChange: (String) -> Unit = {},
    onAddMovementClick: (
        animalReference: String,
        movementInformation: String,
        responsibleWorker: String
    ) -> Unit = { _, _, _ -> },
    onSaveClick: (
        movementInformation: String,
        responsibleWorker: String
    ) -> Unit = { _, _ -> }
) {
    var animalReferenceState by remember(animalReference) { mutableStateOf(animalReference) }
    var destinationState by remember(movementInformation) { mutableStateOf(movementInformation) }
    var workerState by remember(responsibleWorker) { mutableStateOf(responsibleWorker) }
    var movementType by remember { mutableStateOf("Within Site") }
    var notes by remember { mutableStateOf("") }

    val latestMovement = movementRecords.maxByOrNull { it.timestamp }
    val fromLocation = latestMovement?.destinationFarmId?.takeIf { it.isNotBlank() } ?: "Main site"
    val today = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeeftechBackground)
            .verticalScroll(rememberScrollState())
    ) {
        TraceabilityHeader(
            eyebrow = "BEEFTECH",
            title = "New Movement",
            subtitle = "Capture where the animal is moving next",
            icon = Icons.Outlined.Route,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MovementStepIndicator()

            TraceabilitySectionTitle("Movement Details")

            TraceabilityCard {
                TraceabilityTextField(
                    label = "Animal",
                    value = animalReferenceState,
                    onValueChange = { value ->
                        animalReferenceState = value
                        onAnimalReferenceChange(value)
                    },
                    icon = Icons.Outlined.Pets
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Movement Type *",
                    fontWeight = FontWeight.SemiBold,
                    color = BeeftechText
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MovementTypes.forEach { option ->
                        MovementTypeCard(
                            option = option,
                            selected = movementType == option.label,
                            onClick = {
                                movementType = option.label
                                if (option.label == "Sold" && destinationState.isBlank()) {
                                    destinationState = "Buyer / destination"
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TraceabilityTextField(
                    label = "Date",
                    value = today,
                    onValueChange = {},
                    icon = Icons.Outlined.CalendarMonth
                )

                Spacer(modifier = Modifier.height(14.dp))

                TraceabilityTextField(
                    label = "From Location",
                    value = fromLocation,
                    onValueChange = {},
                    icon = Icons.Outlined.Home
                )

                Spacer(modifier = Modifier.height(14.dp))

                TraceabilityTextField(
                    label = when (movementType) {
                        "Sold" -> "Buyer / Destination *"
                        "Another Site" -> "Destination Site *"
                        "Other" -> "Destination / Outcome *"
                        else -> "To Location *"
                    },
                    value = destinationState,
                    onValueChange = { value ->
                        destinationState = value
                        onMovementInformationChange(value)
                    },
                    icon = Icons.Outlined.Route
                )

                Spacer(modifier = Modifier.height(14.dp))

                TraceabilitySearchableDropdown(
                    label = "Responsible Worker",
                    value = workerState,
                    options = workerOptions,
                    icon = Icons.Outlined.Person,
                    onValueChange = { value ->
                        workerState = value
                        onResponsibleWorkerChange(value)
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                TraceabilityTextField(
                    label = "Notes (optional)",
                    value = notes,
                    onValueChange = { notes = it },
                    icon = Icons.Outlined.MoreHoriz,
                    singleLine = false,
                    minLines = 2
                )
            }

            TraceabilityPrimaryButton(
                text = "Save Movement",
                icon = Icons.Outlined.Route,
                onClick = {
                    val destination = destinationState.trim()
                    val encoded = buildString {
                        when (movementType) {
                            "Within Site" -> append(destination)
                            "Another Site" -> append("Another site: $destination")
                            "Sold" -> append("Sold: $destination")
                            else -> append("Other: $destination")
                        }
                        if (notes.isNotBlank()) append(" · ${notes.trim()}")
                    }
                    onSaveClick(encoded.trim(), workerState.trim())
                }
            )

            Spacer(modifier = Modifier.height(4.dp))
            TraceabilitySectionTitle("Movement History")

            if (movementRecords.isEmpty() && foundAnimalReference.isBlank()) {
                TraceabilityCard {
                    Text(
                        text = "No movement records yet. Saved movements for this animal will appear here.",
                        color = BeeftechMutedText
                    )
                }
            } else {
                movementRecords.forEach { movement ->
                    TraceabilityCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BeeftechSoftAccent
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Route,
                                    contentDescription = null,
                                    tint = BeeftechPrimaryDark,
                                    modifier = Modifier.padding(10.dp).size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.size(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = movement.movementType,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BeeftechText
                                )
                                Text(
                                    text = movement.responsibleWorker.ifBlank { "Worker not recorded" },
                                    color = BeeftechMutedText
                                )
                            }
                            Text(
                                text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(movement.timestamp)),
                                color = BeeftechMutedText
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MovementStepIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        listOf("1" to "Details", "2" to "Location", "3" to "Confirm").forEachIndexed { index, item ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = if (index == 0) BeeftechPrimaryDark else Color(0xFFE2E5E3)
                ) {
                    Text(
                        text = item.first,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = if (index == 0) Color.White else BeeftechMutedText,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(item.second, color = BeeftechMutedText, modifier = Modifier.padding(top = 4.dp))
            }
            if (index < 2) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .height(2.dp)
                        .weight(1f)
                        .background(Color(0xFFD9DDD8))
                )
            }
        }
    }
}

@Composable
private fun MovementTypeCard(
    option: MovementTypeOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(88.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) BeeftechPrimaryDark else Color(0xFFF6F7F6)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = if (selected) Color.White else BeeftechMutedText,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = option.label,
                color = if (selected) Color.White else BeeftechText,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AnimalMovementScreenPreview() {
    AnimalMovementScreen(animalReference = "ZA100123")
}
