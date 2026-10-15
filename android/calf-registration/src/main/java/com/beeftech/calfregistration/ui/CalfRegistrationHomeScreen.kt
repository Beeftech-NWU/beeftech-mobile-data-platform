package com.beeftech.calfregistration.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val RECENT_CALF_COUNT = 5

@Composable
fun CalfRegistrationHomeScreen(
    registeredCalves: List<CalfRegistrationData>,
    onRegisterCalfClick: () -> Unit,
    onViewRegisteredClick: () -> Unit,
    onSelectCalf: (CalfRegistrationData) -> Unit
) {
    val total = registeredCalves.size
    val syncedCount = registeredCalves.count { it.synced }
    val attentionCount = registeredCalves.count { it.needsAttention }
    val pendingCount = total - syncedCount - attentionCount

    Column(
        modifier = Modifier.fillMaxSize().background(BeeftechBackground).verticalScroll(rememberScrollState())
    ) {
        CalfHeader(
            title = "Calf Registration",
            subtitle = (if (total == 1) "1 calf registered" else "$total calves registered") +
                " · stored on this device",
            icon = Icons.Outlined.Pets
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RegisterCalfHeroCard(onClick = onRegisterCalfClick)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SyncStatTile(count = syncedCount, label = "Synced", modifier = Modifier.weight(1f))
                SyncStatTile(count = pendingCount, label = "Pending", modifier = Modifier.weight(1f))
                if (attentionCount > 0) {
                    SyncStatTile(
                        count = attentionCount,
                        label = "Need attention",
                        modifier = Modifier.weight(1f),
                        highlight = true
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    CalfSectionTitle("Recently registered")
                }
                TextButton(onClick = onViewRegisteredClick) {
                    Text(
                        text = if (total > 0) "View all ($total) ›" else "View all ›",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BeeftechPrimary
                    )
                }
            }

            if (registeredCalves.isEmpty()) {
                CalfCard {
                    Text(
                        text = "No calves registered yet. Tap Register a calf to capture the first animal.",
                        fontSize = 13.sp,
                        color = BeeftechMutedText,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    registeredCalves.take(RECENT_CALF_COUNT).forEach { calf ->
                        CalfRegisteredItemCard(calf = calf, onClick = { onSelectCalf(calf) })
                    }
                }
            }
        }
    }
}

/** The main action on this screen: a large filled card so it reads as the primary task. */
@Composable
private fun RegisterCalfHeroCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BeeftechPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(BeeftechWhite.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.AddCircleOutline,
                    contentDescription = null,
                    tint = BeeftechWhite,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Register a calf", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BeeftechWhite)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Tag, details, condition and parentage",
                    fontSize = 13.sp,
                    color = BeeftechWhite.copy(alpha = 0.8f)
                )
            }
            Text("›", fontSize = 30.sp, fontWeight = FontWeight.Medium, color = BeeftechWhite)
        }
    }
}

@Composable
private fun SyncStatTile(
    count: Int,
    label: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    val accent = if (highlight) Color(0xFF8C1D18) else BeeftechPrimaryDark
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (highlight) Color(0xFFF9DAD7) else BeeftechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(text = count.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accent)
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = BeeftechMutedText, maxLines = 1)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalfRegistrationHomeScreenPreview() {
    CalfRegistrationHomeScreen(
        registeredCalves = CalfRegistrationLookups.initialRegisteredCalves,
        onRegisterCalfClick = {},
        onViewRegisteredClick = {},
        onSelectCalf = {}
    )
}
