package com.beeftech.demoapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.beeftech.authentication.domain.Role
import com.beeftech.demoapp.ui.theme.BeefAccent
import com.beeftech.demoapp.ui.theme.BeefBackground
import com.beeftech.demoapp.ui.theme.BeefBorder
import com.beeftech.demoapp.ui.theme.BeefDanger
import com.beeftech.demoapp.ui.theme.BeefMutedText
import com.beeftech.demoapp.ui.theme.BeefOffline
import com.beeftech.demoapp.ui.theme.BeefPrimary
import com.beeftech.demoapp.ui.theme.BeefPrimaryStrong
import com.beeftech.demoapp.ui.theme.BeefSoftGreen
import com.beeftech.demoapp.ui.theme.BeefSoftSurface
import com.beeftech.demoapp.ui.theme.BeefSuccess
import com.beeftech.demoapp.ui.theme.BeefSurface
import com.beeftech.demoapp.ui.theme.BeefText
import com.beeftech.demoapp.ui.theme.BeefWarning

private val SyncWaitingBackground = Color(0xFFFCE8C3)
private val SyncFailedBackground = Color(0xFFF9DAD7)
private val SyncOfflineBackground = Color(0xFFE3E6E4)

enum class SyncTone {
    SYNCED,
    WAITING,
    OFFLINE,
    FAILED
}

data class AppSyncUiState(
    val tone: SyncTone,
    val label: String,
    val detail: String
)

fun appSyncUiState(
    isOnline: Boolean,
    pendingCount: Int,
    failedCount: Int
): AppSyncUiState =
    when {
        !isOnline ->
            AppSyncUiState(
                tone = SyncTone.OFFLINE,
                label = "Sync paused",
                detail = if (pendingCount > 0) {
                    "$pendingCount record${if (pendingCount == 1) "" else "s"} waiting for internet"
                } else {
                    "Working offline - changes will sync when connected"
                }
            )

        failedCount > 0 ->
            AppSyncUiState(
                tone = SyncTone.FAILED,
                label = "Needs attention",
                detail = "$failedCount record${if (failedCount == 1) "" else "s"} could not sync"
            )

        pendingCount > 0 ->
            AppSyncUiState(
                tone = SyncTone.WAITING,
                label = "Waiting to sync",
                detail = "$pendingCount record${if (pendingCount == 1) "" else "s"} queued"
            )

        else ->
            AppSyncUiState(
                tone = SyncTone.SYNCED,
                label = "Synced",
                detail = "Everything on this device is up to date"
            )
    }

@Composable
fun rememberIsOnline(): State<Boolean> {
    val context = LocalContext.current
    val manager = remember(context) {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    val online = remember(manager) {
        mutableStateOf(manager.hasUsableNetwork())
    }

    DisposableEffect(manager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                online.value = manager.hasUsableNetwork()
            }

            override fun onLost(network: Network) {
                online.value = manager.hasUsableNetwork()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                online.value = manager.hasUsableNetwork()
            }
        }

        val registered = runCatching {
            manager.registerDefaultNetworkCallback(callback)
            true
        }.getOrDefault(false)

        online.value = manager.hasUsableNetwork()

        onDispose {
            if (registered) {
                runCatching { manager.unregisterNetworkCallback(callback) }
            }
        }
    }

    return online
}

private fun ConnectivityManager.hasUsableNetwork(): Boolean {
    val network = activeNetwork ?: return false
    val capabilities = getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Composable
fun BeefAppHeader(
    title: String,
    username: String,
    siteId: String?,
    syncState: AppSyncUiState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = BeefPrimary,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "BEEFTECH  ·  ${title.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = buildString {
                            append(username)
                            val site = siteId?.trim().orEmpty()
                            if (site.isNotEmpty()) append("  ·  $site")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.size(12.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NetworkStatusChip(
                        isOnline = syncState.tone != SyncTone.OFFLINE
                    )
                    SyncStatusChip(syncState)
                }
            }
        }
    }
}

@Composable
fun NetworkStatusChip(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val background =
        if (isOnline) {
            Color(0xFFDFF3E5)
        } else {
            Color(0xFFF9DAD7)
        }

    val foreground =
        if (isOnline) {
            BeefSuccess
        } else {
            BeefDanger
        }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = background
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier =
                    Modifier
                        .size(7.dp)
                        .background(
                            color = foreground,
                            shape = CircleShape
                        )
            )

            Text(
                text = if (isOnline) "Online" else "Offline",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = foreground
            )
        }
    }
}

@Composable
fun SyncStatusChip(
    state: AppSyncUiState,
    modifier: Modifier = Modifier
) {
    val (background, foreground, icon) =
        when (state.tone) {
            SyncTone.SYNCED -> Triple(BeefSoftGreen, BeefSuccess, Icons.Outlined.CloudDone)
            SyncTone.WAITING -> Triple(SyncWaitingBackground, BeefWarning, Icons.Outlined.CloudQueue)
            SyncTone.OFFLINE -> Triple(SyncOfflineBackground, BeefOffline, Icons.Outlined.CloudOff)
            SyncTone.FAILED -> Triple(SyncFailedBackground, BeefDanger, Icons.Outlined.SyncProblem)
        }

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
                text = state.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = foreground,
                maxLines = 1
            )
        }
    }
}

@Composable
fun BeefBottomNavigation(
    tabs: List<AppTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = BeefSurface,
        tonalElevation = 8.dp
    ) {
        tabs.forEachIndexed { index, tab ->
            val icon = when (tab) {
                AppTab.HOME -> Icons.Outlined.Home
                AppTab.CALF_REGISTRATION -> Icons.Outlined.Pets
                AppTab.TRACEABILITY -> Icons.Outlined.Route
                AppTab.FEED_CRIB -> Icons.Outlined.Restaurant
                else -> Icons.Outlined.MoreHoriz
            }

            NavigationBarItem(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BeefPrimaryStrong,
                    selectedTextColor = BeefPrimaryStrong,
                    indicatorColor = BeefSoftGreen,
                    unselectedIconColor = BeefMutedText,
                    unselectedTextColor = BeefMutedText
                )
            )
        }
    }
}

@Composable
private fun SyncProgressVisual(
    pendingCount: Int,
    syncState: AppSyncUiState,
    modifier: Modifier = Modifier
) {
    var batchTotal by remember { mutableIntStateOf(pendingCount.coerceAtLeast(0)) }

    LaunchedEffect(pendingCount, syncState.tone) {
        when {
            syncState.tone == SyncTone.SYNCED -> batchTotal = 0
            syncState.tone == SyncTone.WAITING && pendingCount > batchTotal -> batchTotal = pendingCount
        }
    }

    val percent = when {
        syncState.tone == SyncTone.SYNCED -> 100
        syncState.tone == SyncTone.WAITING && batchTotal > 0 ->
            (((batchTotal - pendingCount).coerceAtLeast(0) * 100f) / batchTotal)
                .toInt()
                .coerceIn(0, 99)
        else -> 0
    }

    val progressColor = when (syncState.tone) {
        SyncTone.SYNCED -> BeefSuccess
        SyncTone.WAITING -> BeefPrimaryStrong
        SyncTone.OFFLINE -> BeefOffline
        SyncTone.FAILED -> BeefDanger
    }

    val headline = when (syncState.tone) {
        SyncTone.SYNCED -> "Everything synced"
        SyncTone.WAITING -> "Syncing..."
        SyncTone.OFFLINE -> "You are offline"
        SyncTone.FAILED -> "Sync needs attention"
    }

    val message = when (syncState.tone) {
        SyncTone.SYNCED -> "All records on this device are up to date."
        SyncTone.WAITING ->
            if (pendingCount == 1) "Uploading 1 record" else "Uploading $pendingCount records"
        SyncTone.OFFLINE ->
            if (pendingCount > 0) {
                "$pendingCount record${if (pendingCount == 1) "" else "s"} will sync automatically when you are back online."
            } else {
                "Records will sync automatically when you are back online."
            }
        SyncTone.FAILED -> syncState.detail
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BeefText
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )
        }

        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = BeefMutedText
        )

        LinearProgressIndicator(
            progress = percent / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = progressColor,
            trackColor = BeefBorder
        )
    }
}
@Composable
fun BeefHomeScreen(
    username: String,
    siteId: String?,
    role: Role?,
    pendingCount: Int,
    syncState: AppSyncUiState,
    onRegisterCalf: () -> Unit,
    onTraceability: () -> Unit,
    onFeed: () -> Unit,
    onDashboard: () -> Unit,
    onReports: () -> Unit,
    onMyActivity: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BeefBackground),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = BeefSoftSurface),
                border = BorderStroke(1.dp, BeefBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Welcome back, $username",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = BeefText
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = siteId?.takeIf { it.isNotBlank() } ?: "Your BeefTech workspace",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BeefMutedText
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRegisterCalf,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BeefPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Outlined.Pets, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Register calf", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item { HomeSectionTitle("Quick actions", "Get to common farm tasks with one tap") }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeActionCard(
                    title = "Traceability",
                    subtitle = "Find animals and record events",
                    icon = Icons.Outlined.Route,
                    onClick = onTraceability,
                    modifier = Modifier.weight(1f)
                )
                HomeActionCard(
                    title = "Feed",
                    subtitle = "Cribs and feed readings",
                    icon = Icons.Outlined.Restaurant,
                    onClick = onFeed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (role == Role.MANAGER || role == Role.ADMIN) {
            item { HomeSectionTitle("Farm management", "Monitor activity and review records") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeActionCard(
                        title = "Dashboard",
                        subtitle = "Farm metrics and alerts",
                        icon = Icons.Outlined.Dashboard,
                        onClick = onDashboard,
                        modifier = Modifier.weight(1f)
                    )
                    HomeActionCard(
                        title = "Reports",
                        subtitle = "Review and share reports",
                        icon = Icons.Outlined.Assessment,
                        onClick = onReports,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item { HomeSectionTitle("Sync", "Know what is safely stored and what still needs the server") }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onMyActivity),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BeefSurface),
                border = BorderStroke(1.dp, BeefBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SyncStatusChip(syncState)
                        Text(
                            text = "›",
                            style = MaterialTheme.typography.headlineSmall,
                            color = BeefAccent
                        )
                    }

                    SyncProgressVisual(
                        pendingCount = pendingCount,
                        syncState = syncState
                    )

                    Text(
                        text = if (pendingCount > 0) {
                            "Tap to review pending activity"
                        } else {
                            "Tap to view your activity"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = BeefMutedText
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(6.dp)) }
    }
}

@Composable
private fun HomeSectionTitle(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = BeefText
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = BeefMutedText
        )
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .defaultMinSize(minHeight = 132.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BeefSurface),
        border = BorderStroke(1.dp, BeefBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(BeefSoftGreen, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BeefPrimaryStrong,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BeefText
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = BeefMutedText
            )
        }
    }
}

@Composable
fun BeefMoreScreen(
    role: Role?,
    onOpen: (AppTab) -> Unit,
    onMyActivity: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val managementItems = moreTabsFor(role)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BeefBackground),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HomeSectionTitle(
                title = "More",
                subtitle = "Activity, management tools and account actions"
            )
        }

        item {
            MoreActionCard(
                title = "My activity",
                subtitle = "Review pending sync and recent device activity",
                icon = Icons.Outlined.Badge,
                onClick = onMyActivity
            )
        }

        managementItems.forEach { destination ->
            item(key = destination.name) {
                MoreActionCard(
                    title = destination.label,
                    subtitle = destination.moreDescription(),
                    icon = destination.moreIcon(),
                    onClick = { onOpen(destination) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            MoreActionCard(
                title = "Log out",
                subtitle = "End this session on the device",
                icon = Icons.Outlined.Logout,
                onClick = onLogout,
                danger = true
            )
        }
    }
}

@Composable
private fun MoreActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 76.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BeefSurface),
        border = BorderStroke(1.dp, if (danger) BeefDanger.copy(alpha = 0.28f) else BeefBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (danger) SyncFailedBackground else BeefSoftGreen,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (danger) BeefDanger else BeefPrimaryStrong,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (danger) BeefDanger else BeefText
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = BeefMutedText
                )
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = if (danger) BeefDanger else BeefAccent)
        }
    }
}

private fun AppTab.moreDescription(): String =
    when (this) {
        AppTab.DASHBOARD -> "Farm metrics, costs, team and operational alerts"
        AppTab.REPORTS -> "Create, review and share farm reports"
        AppTab.RECORDS -> "Review captured records and corrections"
        AppTab.TEAM -> "Workers, phones and team activity"
        AppTab.ADMIN -> "Sites, reference data, security and system controls"
        else -> "Open ${label.lowercase()}"
    }

private fun AppTab.moreIcon(): ImageVector =
    when (this) {
        AppTab.DASHBOARD -> Icons.Outlined.Dashboard
        AppTab.REPORTS -> Icons.Outlined.Assessment
        AppTab.RECORDS -> Icons.Outlined.Description
        AppTab.TEAM -> Icons.Outlined.SupervisorAccount
        AppTab.ADMIN -> Icons.Outlined.VerifiedUser
        else -> Icons.Outlined.MoreHoriz
    }
