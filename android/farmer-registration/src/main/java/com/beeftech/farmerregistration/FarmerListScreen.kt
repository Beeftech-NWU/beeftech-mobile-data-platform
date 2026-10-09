package com.beeftech.farmerregistration

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.entity.FarmerEntity
import com.beeftech.database.repository.FarmerRepository
import com.beeftech.farmerregistration.ui.theme.BeeftechTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** The status pill for a farmer's local sync status. Anything unknown shows as Pending Sync. */
enum class FarmerSyncPill(val label: String) {
    PENDING_SYNC("Pending Sync"),
    PROCESSING("Processing"),
    REGISTERED("Registered");

    companion object {
        fun forStatus(syncStatus: String?): FarmerSyncPill =
            when (syncStatus?.trim()?.uppercase()) {
                "PROCESSING" -> PROCESSING
                "SYNCED" -> REGISTERED
                else -> PENDING_SYNC
            }
    }
}

/**
 * Every farmer registered on this device with its sync status. The list follows the database,
 * so pills change as FarmerSyncWorker works through the queue.
 */
class FarmerListScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val farmers: Flow<List<FarmerEntity>>? =
            DatabaseProvider.getDatabase()
                ?.let { FarmerRepository(it.farmerDao()).observeAllFarmers() }

        setContent {

            BeeftechTheme {

                val list by remember { farmers ?: flowOf(emptyList()) }
                    .collectAsState(initial = null)

                FarmerListContent(
                    farmers = list,
                    databaseAvailable = farmers != null,
                    onBackClick = { finish() },
                    onRegisterClick = {
                        FarmerRegistrationSession.clear()
                        startActivity(Intent(this@FarmerListScreen, ClientDetailsScreen::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun FarmerListContent(
    farmers: List<FarmerEntity>?,
    databaseAvailable: Boolean,
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BeeftechBackground)
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BeeftechPrimaryDeep)
                    .padding(start = 14.dp, end = 22.dp, top = 44.dp, bottom = 20.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                IconButton(onClick = onBackClick, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = BeeftechWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column {
                    Text(
                        text = "FARMER REGISTRATION",
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BeeftechPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Registered Farmers",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = BeeftechWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(17.dp))

            HorizontalDivider(thickness = 2.dp, color = BeeftechPrimary)
        }

        Box(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp)) {
            FarmerPrimaryButton(text = "Register farmer", onClick = onRegisterClick)
        }

        when {
            !databaseAvailable ->
                FarmerListMessage("The local database is not available. Sign in again to see farmers.")

            farmers == null ->
                Unit

            farmers.isEmpty() ->
                FarmerListMessage("No farmers registered on this device yet.")

            else ->
                LazyColumn(
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(farmers, key = { it.farmer_id }) { farmer ->
                        FarmerRow(farmer)
                    }
                }
        }
    }
}

@Composable
private fun FarmerListMessage(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = BeeftechMutedText,
        modifier = Modifier.padding(18.dp)
    )
}

@Composable
private fun FarmerRow(farmer: FarmerEntity) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BeeftechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = farmer.organisation_name?.ifBlank { null } ?: "Unnamed organisation",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BeeftechText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text =
                        listOfNotNull(
                            farmer.contact_name?.ifBlank { null },
                            farmer.client_code?.ifBlank { null }
                        ).joinToString(" · ").ifBlank { "No contact or client code" },
                    fontSize = 12.sp,
                    color = BeeftechMutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            FarmerSyncStatusPill(FarmerSyncPill.forStatus(farmer.sync_status))
        }
    }
}

/* Same shape and type as the demo app's SyncStatusChip, which this module cannot import. */
@Composable
fun FarmerSyncStatusPill(
    pill: FarmerSyncPill,
    modifier: Modifier = Modifier
) {
    val (background, foreground, icon) =
        when (pill) {
            FarmerSyncPill.PENDING_SYNC -> Triple(PillPendingBackground, PillPendingForeground, Icons.Outlined.CloudQueue)
            FarmerSyncPill.PROCESSING -> Triple(PillProcessingBackground, PillProcessingForeground, Icons.Outlined.Sync)
            FarmerSyncPill.REGISTERED -> Triple(PillRegisteredBackground, PillRegisteredForeground, Icons.Outlined.CloudDone)
        }

    PillSurface(pill.label, background, foreground, icon, modifier)
}

@Composable
private fun PillSurface(
    label: String,
    background: Color,
    foreground: Color,
    icon: ImageVector,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = background
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = foreground,
                maxLines = 1
            )
        }
    }
}

/* Amber and green match the demo app's waiting and synced chips; blue marks a sync in flight. */
private val PillPendingBackground = Color(0xFFFCE8C3)
private val PillPendingForeground = Color(0xFF7A5A20)
private val PillProcessingBackground = Color(0xFFDCE8F5)
private val PillProcessingForeground = Color(0xFF2F5B8A)
private val PillRegisteredBackground = Color(0xFFE3E8E2)
private val PillRegisteredForeground = Color(0xFF3F6A50)

@Preview(showBackground = true)
@Composable
fun FarmerListScreenPreview() {

    fun farmer(id: String, name: String, contact: String?, status: String) =
        FarmerEntity(
            farmer_id = id,
            client_code = id.uppercase(),
            organisation_name = name,
            vat_number = null,
            email_address = null,
            gps_latitude = null,
            gps_longitude = null,
            sync_status = status,
            contact_name = contact
        )

    BeeftechTheme {
        FarmerListContent(
            farmers =
                listOf(
                    farmer("kar003", "Karoo Vryburg", "Jan Botha", "PENDING"),
                    farmer("kar002", "Alpha Cattle Farms", null, "PROCESSING"),
                    farmer("kar001", "Bosveld Beef", "Thandi Nkosi", "SYNCED")
                ),
            databaseAvailable = true,
            onBackClick = {},
            onRegisterClick = {}
        )
    }
}
