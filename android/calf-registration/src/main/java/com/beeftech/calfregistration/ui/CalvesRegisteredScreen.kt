package com.beeftech.calfregistration.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalvesRegisteredScreen(
    registeredCalves: List<CalfRegistrationData>,
    onSelectCalf: (CalfRegistrationData) -> Unit,
    onRegisterNewCalfClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeeftechBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        CalfHeader(
            timeString = "07:25",
            unsyncedCount = 1,
            eyebrow = "REGISTER NEW CALF • EXTENDED",
            title = "CALVES REGISTERED",
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            // Date Range Picker Bar (24 Aug -> 29 Aug)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = BeeftechSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BeeftechWhite)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "24 Aug",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BeeftechText
                        )
                    }

                    Text(
                        text = "→",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BeeftechMutedText
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BeeftechWhite)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "29 Aug",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BeeftechText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calf List
            registeredCalves.forEach { calf ->
                CalfRegisteredItemCard(
                    calf = calf,
                    onClick = { onSelectCalf(calf) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Re-opening note text
            Text(
                text = "Selecting a line reopens it exactly as entered — nothing is editable here without going back to the full form.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = BeeftechMutedText,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Button: Register New Calf
            CalfPrimaryButton(
                text = "+ REGISTER NEW CALF",
                onClick = onRegisterNewCalfClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Design Note
            CalfDesignNoteCard(
                noteText = "Tag Number, Type and Gender are the three columns the spec calls for (4d.ii.2) — enough to spot a duplicate or mistake at a glance without opening every record."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CalfRegisteredItemCard(
    calf: CalfRegistrationData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BeeftechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = calf.tagNumber,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = BeeftechText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${calf.animalType.split("—").last().trim()} • ${calf.gender}",
                    fontSize = 12.sp,
                    color = BeeftechMutedText
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(BeeftechInputBeige)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "View",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BeeftechText
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalvesRegisteredScreenPreview() {
    CalvesRegisteredScreen(
        registeredCalves = CalfRegistrationLookups.initialRegisteredCalves,
        onSelectCalf = {},
        onRegisterNewCalfClick = {},
        onBackClick = {}
    )
}
