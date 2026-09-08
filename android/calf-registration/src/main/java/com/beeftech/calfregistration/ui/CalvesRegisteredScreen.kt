package com.beeftech.calfregistration.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        modifier = Modifier.fillMaxSize().background(BeeftechBackground).verticalScroll(rememberScrollState())
    ) {
        CalfHeader(
            eyebrow = "Calf registration",
            title = "Registered Calves",
            subtitle = "Review calves registered during this session.",
            icon = Icons.Outlined.Pets,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            CalfSectionTitle("This session")
            Spacer(modifier = Modifier.height(12.dp))
            CalfCard {
                Text(
                    text = "24 Aug – 29 Aug",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BeeftechText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${registeredCalves.size} calves available to review",
                    fontSize = 12.sp,
                    color = BeeftechMutedText
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            CalfSectionTitle("Registered calves")
            Spacer(modifier = Modifier.height(12.dp))
            registeredCalves.forEach { calf ->
                CalfRegisteredItemCard(calf = calf, onClick = { onSelectCalf(calf) })
                Spacer(modifier = Modifier.height(11.dp))
            }

            Spacer(modifier = Modifier.height(15.dp))
            CalfPrimaryButton(text = "Register new calf", onClick = onRegisterNewCalfClick)
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun CalfRegisteredItemCard(calf: CalfRegistrationData, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BeeftechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(46.dp).background(BeeftechSoftAccent, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Tag, contentDescription = null, tint = BeeftechPrimaryDark, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(calf.tagNumber, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BeeftechText)
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${calf.animalType.substringAfter("—", calf.animalType).trim()} • ${calf.gender}",
                    fontSize = 11.sp,
                    color = BeeftechMutedText
                )
            }
            Text("›", fontSize = 26.sp, fontWeight = FontWeight.Medium, color = BeeftechPrimaryDark)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalvesRegisteredScreenPreview() {
    CalvesRegisteredScreen(CalfRegistrationLookups.initialRegisteredCalves, {}, {}, {})
}
