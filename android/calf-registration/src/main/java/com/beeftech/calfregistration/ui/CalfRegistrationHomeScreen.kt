package com.beeftech.calfregistration.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalfRegistrationHomeScreen(
    registeredCount: Int,
    onRegisterCalfClick: () -> Unit,
    onViewRegisteredClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(BeeftechBackground).verticalScroll(rememberScrollState())
    ) {
        CalfHeader(
            eyebrow = "Calf registration",
            title = "Calf Registration",
            subtitle = "Register new calves or review calves registered on this device.",
            icon = Icons.Outlined.Pets
        )

        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            CalfCard {
                Text(
                    text = if (registeredCount == 1) "1 calf registered" else "$registeredCount calves registered",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BeeftechText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Stored on this device", fontSize = 12.sp, color = BeeftechMutedText)
            }

            Spacer(modifier = Modifier.height(24.dp))
            CalfSectionTitle("Options")
            Spacer(modifier = Modifier.height(12.dp))
            CalfMenuCard(
                title = "Register a calf",
                subtitle = "Capture tag, identity, appearance and parentage.",
                icon = Icons.Outlined.AddCircleOutline,
                onClick = onRegisterCalfClick
            )
            Spacer(modifier = Modifier.height(11.dp))
            CalfMenuCard(
                title = "View registered calves",
                subtitle = "Search and review registered calves.",
                icon = Icons.Outlined.ListAlt,
                onClick = onViewRegisteredClick
            )
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalfRegistrationHomeScreenPreview() {
    CalfRegistrationHomeScreen(registeredCount = 3, onRegisterCalfClick = {}, onViewRegisteredClick = {})
}
