package com.beeftech.feedcrib.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@Composable
fun CribDetailScreen(
    penName: String,
    adiValue: Float,
    onAdiChange: (Float) -> Unit,
    readings: List<CribReading>,
    onReadingsChange: (List<CribReading>) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var newReading by remember { mutableStateOf("") }

    // Get the last 9 readings (or fewer if not enough)
    val lastReadings = if (readings.size >= 9) readings.takeLast(9) else readings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FeedCribColors.LightBg)
            .padding(16.dp)
    ) {
        // --- Back button ---
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = FeedCribColors.DarkText
            )
        }

        // --- Pen name as title ---
        Text(
            penName,
            color = FeedCribColors.DarkText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )
        Spacer(modifier = Modifier.height(8.dp))

        // --- Pen info card (read-only data) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = FeedCribColors.LightSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Description", color = FeedCribColors.MutedText, fontSize = 11.sp)
                    Text("GENEING", color = FeedCribColors.DarkText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ration", color = FeedCribColors.MutedText, fontSize = 11.sp)
                    Text("Grower", color = FeedCribColors.DarkText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Method", color = FeedCribColors.MutedText, fontSize = 11.sp)
                    Text("Voldag voeding", color = FeedCribColors.DarkText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Required", color = FeedCribColors.MutedText, fontSize = 11.sp)
                    Text("0.00 kg", color = FeedCribColors.DarkText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("A.D.I", color = FeedCribColors.MutedText, fontSize = 11.sp)
                    Text(
                        "${String.format("%.2f", adiValue)} kg",
                        color = FeedCribColors.RustDeep,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // --- Animal tally ---
        Text(
            "Animals",
            color = FeedCribColors.RustDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(FeedCribColors.InkLight)
        ) {
            listOf("Begin" to "104", "In" to "0", "Out" to "0", "Close" to "104").forEach { (label, value) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(label, color = FeedCribColors.MutedText, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text(value, color = FeedCribColors.RustDeep, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- Reading section ---
        Text(
            "Crib Reading",
            color = FeedCribColors.RustDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )

        // Display last 9 readings in blocks of 3
        if (lastReadings.isNotEmpty()) {
            Text(
                "Last ${lastReadings.size} readings:",
                color = FeedCribColors.MutedText,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Display readings in blocks of 3 (Morning, Mid-Day, Evening)
            lastReadings.chunked(3).forEach { block ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Morning, Mid-Day, Evening labels
                    listOf("M", "D", "E").forEachIndexed { index, label ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = when (label) {
                                "M" -> Color(0xFFE8F5E9)  // light green
                                "D" -> Color(0xFFFFF3E0)  // light orange
                                else -> Color(0xFFF3E5F5)  // light purple
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            val value = if (index < block.size) block[index] else null
                            val reading = if (value != null) {
                                when (index) {
                                    0 -> value.morning
                                    1 -> value.midDay
                                    else -> value.evening
                                }
                            } else "-"

                            Text(
                                "$label: $reading",
                                color = FeedCribColors.DarkText,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                "No readings yet",
                color = FeedCribColors.MutedText,
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // --- Enter new reading with TIME-BASED ALLOCATION ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newReading,
                onValueChange = { newReading = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter code", fontSize = 12.sp) },
                textStyle = androidx.compose.ui.text.TextStyle(
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = FeedCribColors.DarkText
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FeedCribColors.Rust,
                    unfocusedBorderColor = FeedCribColors.Line,
                    focusedTextColor = FeedCribColors.DarkText,
                    unfocusedTextColor = FeedCribColors.DarkText
                )
            )
            Button(
                onClick = {
                    if (newReading.isNotBlank()) {
                        // TIME-BASED ALLOCATION RULES
                        val calendar = Calendar.getInstance()
                        val hour = calendar.get(Calendar.HOUR_OF_DAY)

                        // Determine which column to add to based on current time
                        val timeSlot = when {
                            hour < 11 -> "morning"   // Before 11:00 → Morning
                            hour < 14 -> "midDay"    // Before 14:00 → Mid-Day
                            hour < 20 -> "evening"   // Before 20:00 → Evening
                            else -> "evening"        // After 20:00 → Evening
                        }

                        // Create a new reading with the code in the correct slot
                        val newReadingData = when (timeSlot) {
                            "morning" -> CribReading(newReading, "", "")
                            "midDay" -> CribReading("", newReading, "")
                            else -> CribReading("", "", newReading)
                        }

                        // Add to readings list
                        val updatedReadings = readings + newReadingData
                        onReadingsChange(updatedReadings)

                        // Show feedback with the time slot and hour
                        Toast.makeText(
                            context,
                            "Added to ${timeSlot.uppercase()} (${String.format("%02d:00", hour)})",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Clear the input field
                        newReading = ""
                    } else {
                        Toast.makeText(context, "Please enter a code", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = FeedCribColors.Rust
                )
            ) {
                Text("Add", color = Color.White)
            }
        }

        // Show current time slot info
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeSlotInfo = when {
            currentHour < 11 -> "MORNING (before 11:00)"
            currentHour < 14 -> "MID-DAY (before 14:00)"
            currentHour < 20 -> "EVENING (before 20:00)"
            else -> "EVENING (after 20:00)"
        }
        Text(
            "Current slot: $timeSlotInfo",
            color = FeedCribColors.MutedText,
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // --- ADI stepper ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "A.D.I",
                color = FeedCribColors.RustDeep,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
            Text(
                "Current: ${String.format("%.2f", adiValue)} kg",
                color = FeedCribColors.MutedText,
                fontSize = 10.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decrease button
            IconButton(
                onClick = { onAdiChange(adiValue - 0.1f) },
                modifier = Modifier
                    .size(36.dp)
                    .border(1.5.dp, FeedCribColors.Line, RoundedCornerShape(6.dp))
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = FeedCribColors.RustDeep)
            }
            // Value display
            Text(
                "${String.format("%.2f", adiValue)} kg",
                color = FeedCribColors.DarkText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            // Increase button
            IconButton(
                onClick = { onAdiChange(adiValue + 0.1f) },
                modifier = Modifier
                    .size(36.dp)
                    .border(1.5.dp, FeedCribColors.Line, RoundedCornerShape(6.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", tint = FeedCribColors.RustDeep)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- Save / Discard buttons ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Discard button
            OutlinedButton(
                onClick = {
                    // Reset readings and go back
                    onReadingsChange(emptyList())
                    onBack()
                    Toast.makeText(context, "Changes discarded", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = FeedCribColors.MutedText
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    FeedCribColors.Line
                )
            ) {
                Text("Discard", fontSize = 12.sp)
            }

            // Save button
            Button(
                onClick = {
                    // Save the current readings
                    val count = readings.size
                    Toast.makeText(
                        context,
                        "Saved $count reading(s) for $penName",
                        Toast.LENGTH_LONG
                    ).show()
                    onBack()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FeedCribColors.Rust
                )
            ) {
                Text("Save & Next Crib", fontSize = 12.sp, color = Color.White)
            }
        }
    }
}
