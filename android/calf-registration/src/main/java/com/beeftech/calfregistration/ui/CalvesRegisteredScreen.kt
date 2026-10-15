package com.beeftech.calfregistration.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeftech.database.util.TagNamingUtils

@Composable
fun CalvesRegisteredScreen(
    registeredCalves: List<CalfRegistrationData>,
    onSelectCalf: (CalfRegistrationData) -> Unit,
    onRegisterNewCalfClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredCalves = remember(searchQuery, registeredCalves) {
        if (searchQuery.isBlank()) {
            registeredCalves
        } else {
            val expandedQuery = TagNamingUtils.parseAndExpand(searchQuery)
            registeredCalves.filter { calf ->
                calf.tagNumber.contains(searchQuery.trim(), ignoreCase = true) ||
                calf.tagNumber.contains(expandedQuery, ignoreCase = true) ||
                calf.animalType.contains(searchQuery.trim(), ignoreCase = true) ||
                calf.gender.contains(searchQuery.trim(), ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BeeftechBackground).verticalScroll(rememberScrollState())
    ) {
        val syncedCount = registeredCalves.count { it.synced }
        val attentionCount = registeredCalves.count { it.needsAttention }
        CalfHeader(
            title = "Registered Calves",
            subtitle = "${registeredCalves.size} total · $syncedCount synced · " +
                "${registeredCalves.size - syncedCount - attentionCount} pending" +
                if (attentionCount > 0) " · $attentionCount need attention" else "",
            icon = Icons.Outlined.Pets,
            showBackButton = true,
            onBackClick = onBackClick
        )

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            CalfTextField(
                label = "Search tag (B64 → Blu0000064)",
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search tag, e.g. B64",
                icon = Icons.Outlined.Search
            )

            Spacer(modifier = Modifier.height(12.dp))
            CalfSectionTitle("Registered calves (${filteredCalves.size})")
            Spacer(modifier = Modifier.height(8.dp))

            if (filteredCalves.isEmpty()) {
                CalfCard {
                    Text(
                        text = if (registeredCalves.isEmpty()) "No calves registered yet. Tap Register new calf to capture the first animal."
                        else "No calves found matching '$searchQuery'",
                        fontSize = 13.sp,
                        color = BeeftechMutedText,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                filteredCalves.forEach { calf ->
                    CalfRegisteredItemCard(calf = calf, onClick = { onSelectCalf(calf) })
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            CalfPrimaryButton(text = "Register new calf", onClick = onRegisterNewCalfClick)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun CalfRegisteredItemCard(calf: CalfRegistrationData, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BeeftechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(BeeftechSoftAccent, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Tag, contentDescription = null, tint = BeeftechPrimaryDark, modifier = Modifier.size(19.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(calf.tagNumber, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BeeftechText)
                Text(
                    text = "${calf.animalType.substringAfter("—", calf.animalType).trim()} • ${calf.gender}",
                    fontSize = 11.sp,
                    color = BeeftechMutedText
                )
            }
            Text(
                text = when {
                    calf.needsAttention -> "NEEDS ATTENTION"
                    calf.synced -> "SYNCED"
                    else -> "PENDING"
                },
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = BeeftechPrimaryDark,
                modifier = Modifier
                    .background(BeeftechSoftAccent, RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("›", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = BeeftechPrimaryDark)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalvesRegisteredScreenPreview() {
    CalvesRegisteredScreen(CalfRegistrationLookups.initialRegisteredCalves, {}, {}, {})
}
