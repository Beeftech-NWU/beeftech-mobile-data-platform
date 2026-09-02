package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CalfRegistration
import com.example.ui.components.CalvingEaseChip
import com.example.ui.components.LivestockEarTagVisual
import com.example.ui.components.SexPill
import com.example.ui.components.SyncStatusBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CalfDetailScreen(
    calf: CalfRegistration,
    onNavigateBack: () -> Unit,
    onMarkPending: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val capturedDateStr = dateFormat.format(Date(calf.capturedAt))
    val syncedDateStr = calf.syncedAt?.let { dateFormat.format(Date(it)) } ?: "Not yet synced (Local SQLite)"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = "Calf Passport",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Livestock Biometrics & Traceability Record",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val shareText = """
                            BEEFTECH LIVESTOCK PASSPORT & AUDIT RECORD
                            ============================================
                            Ear Tag Number: ${calf.animalId}
                            Electronic RFID: ${calf.rfidTag ?: "N/A"}
                            Sex: ${calf.sex}
                            Breed: ${calf.breed}
                            Date of Birth: ${calf.birthDate}
                            Birth Weight: ${calf.birthWeightKg ?: "N/A"} kg
                            Calving Ease Score: ${calf.calvingEase} (${when(calf.calvingEase) {
                                1 -> "Normal Unassisted"
                                2 -> "Easy Pull"
                                3 -> "Hard Pull"
                                4 -> "Surgical / Cesarean"
                                else -> "Normal"
                            }})
                            Calf Vigor: ${calf.vigor}
                            Horn Status: ${calf.hornStatus}
                            Pasture / Paddock: ${calf.pastureLocation}
                            Dam ID (Mother): ${calf.damId ?: "N/A"}
                            Sire ID (Bull): ${calf.sireId ?: "N/A"}
                            Sync Status: ${calf.syncStatus}
                            Captured: $capturedDateStr
                            GPS Location: Lat ${calf.gpsLat}, Lng ${calf.gpsLng}
                            Device ID: ${calf.deviceId}
                            Record GUID: ${calf.recordGuid}
                        """.trimIndent()

                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Calf Record"))
                    },
                    modifier = Modifier.testTag("detail_share_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Hero Ear Tag & Status Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        LivestockEarTagVisual(
                            tagNumber = calf.animalId,
                            breed = calf.breed,
                            sex = calf.sex,
                            rfidTag = calf.rfidTag,
                            isLarge = true
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            SyncStatusBadge(status = calf.syncStatus)
                            Spacer(modifier = Modifier.height(10.dp))
                            SexPill(sex = calf.sex)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = calf.breed,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DOB: ${calf.birthDate}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }

                        if (calf.birthWeightKg != null) {
                            Text(
                                text = "${calf.birthWeightKg} kg",
                                color = EarTagYellow,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Section 1: Biometric Scores & Field Health
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Calf Biometrics & Vitality",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4-Grid Biometrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Birth Weight
                        BiometricDataTile(
                            title = "Birth Weight",
                            value = calf.birthWeightKg?.let { "$it kg" } ?: "Not weighed",
                            subtitle = if (calf.birthWeightKg != null && calf.birthWeightKg > 40.0) "Heavy birth" else "Optimal range",
                            modifier = Modifier.weight(1f)
                        )

                        // Calving Ease
                        BiometricDataTile(
                            title = "Calving Ease",
                            value = "Score ${calf.calvingEase}",
                            subtitle = when (calf.calvingEase) {
                                1 -> "Unassisted"
                                2 -> "Easy Pull"
                                3 -> "Hard Pull"
                                4 -> "Surgical / Cesarean"
                                else -> "Normal"
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Vigor
                        BiometricDataTile(
                            title = "Calf Vigor",
                            value = calf.vigor.split("/").first().trim(),
                            subtitle = "Standing & Nursing",
                            modifier = Modifier.weight(1f)
                        )

                        // Horn status
                        BiometricDataTile(
                            title = "Horn Status",
                            value = calf.hornStatus.split("(").first().trim(),
                            subtitle = calf.pastureLocation,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Paddock / Camp Location banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Landscape,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Assigned Pasture:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = calf.pastureLocation,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Section 2: Parentage & Lineage Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pedigree & Parentage",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Dam
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Female,
                                        contentDescription = null,
                                        tint = SexHeiferText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dam (Mother):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = calf.damId?.ifEmpty { "Not specified" } ?: "Not specified",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (calf.damId.isNullOrEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Sire
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Male,
                                        contentDescription = null,
                                        tint = SexBullText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sire (Bull):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = calf.sireId?.ifEmpty { "Not specified" } ?: "Not specified",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (calf.sireId.isNullOrEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Immutable Audit Trail Card (GPS, GUID, Device ID, Timestamps)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Immutable Audit Trail",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE0E7FF))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "POPIA SECURE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3730A3)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AuditDetailRow(
                        icon = Icons.Default.Fingerprint,
                        label = "Record GUID (System Key)",
                        value = calf.recordGuid,
                        isMonospace = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AuditDetailRow(
                        icon = Icons.Default.LocationOn,
                        label = "GPS Coordinates (Point of Capture)",
                        value = "Lat: ${calf.gpsLat}, Lng: ${calf.gpsLng}"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AuditDetailRow(
                        icon = Icons.Default.DeviceHub,
                        label = "Field Terminal Device ID",
                        value = calf.deviceId
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AuditDetailRow(
                        icon = Icons.Default.CalendarToday,
                        label = "Captured Timestamp",
                        value = capturedDateStr
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AuditDetailRow(
                        icon = Icons.Default.CalendarToday,
                        label = "Backend Sync Timestamp",
                        value = syncedDateStr
                    )
                }
            }
        }

        // Action options: Re-sync / Mark Pending
        item {
            if (calf.syncStatus == CalfRegistration.SYNC_STATUS_SYNCED) {
                OutlinedButton(
                    onClick = { onMarkPending(calf.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mark_pending_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Replay, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Re-queue for Sync (Mark as PENDING)")
                }
            }
        }
    }
}

@Composable
fun BiometricDataTile(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AuditDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isMonospace: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
