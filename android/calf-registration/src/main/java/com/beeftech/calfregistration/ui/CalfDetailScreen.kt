package com.beeftech.calfregistration.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalfDetailScreen(
    calf: CalfRegistrationData,
    onBackClick: () -> Unit
) {
    fun parent(value: String) = if (value.startsWith("Select")) "" else value

    Column(
        modifier = Modifier.fillMaxSize().background(BeeftechBackground).verticalScroll(rememberScrollState())
    ) {
        CalfHeader(
            eyebrow = "Calf registration",
            title = calf.tagNumber,
            subtitle = "Registered calf details (read-only).",
            icon = Icons.Outlined.Pets,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            DetailSection(
                "Identity",
                "Tag number" to calf.tagNumber,
                "Old tag number" to calf.oldTagNumber,
                "Transponder" to calf.transponderNumber,
                "Reference" to calf.referenceNumber
            )
            DetailSection(
                "Animal",
                "Type" to calf.animalType,
                "Gender" to calf.gender,
                "Age" to calf.age,
                "Condition" to calf.condition
            )
            DetailSection(
                "Appearance",
                "Hide colour" to calf.hideColour,
                "Conformity" to calf.conformity,
                "Mark" to calf.mark,
                "Group" to calf.group
            )
            DetailSection(
                "Parentage",
                "Dam" to parent(calf.dameTagNumber),
                "Sire" to parent(calf.sireTagNumber)
            )
            DetailSection(
                "Verification",
                "Process proof" to calf.processProof,
                "Implant proof" to calf.implantProof
            )
            DetailSection(
                "Status",
                "Date registered" to calf.dateRegistered,
                "Synced" to if (calf.synced) "Yes" else "Pending"
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun DetailSection(title: String, vararg rows: Pair<String, String>) {
    CalfSectionTitle(title)
    Spacer(modifier = Modifier.height(12.dp))
    CalfCard {
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) Spacer(modifier = Modifier.height(12.dp))
            DetailRow(label, value)
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = BeeftechPrimaryDark
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value.ifBlank { "—" },
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = BeeftechText
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalfDetailScreenPreview() {
    CalfDetailScreen(CalfRegistrationLookups.initialRegisteredCalves.first(), {})
}
