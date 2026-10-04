package com.beeftech.management.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beeftech.management.data.DashboardSummary
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.viewmodel.DashboardViewModel
import com.beeftech.management.viewmodel.DashboardViewModelFactory
import java.text.NumberFormat
import java.util.Locale


private val BeefGreen = Color(0xFF4F6256)
private val BeefDarkGreen = Color(0xFF4F6256)
private val BeefDeepGreen = Color(0xFF4F6256)

private val BeefBackground = Color(0xFFFAF9F2)
private val BeefCard = Color.White

private val BeefLightGreen = Color(0xFFE7F4EC)
private val BeefMint = Color(0xFFF0F8F3)

private val BeefText = Color(0xFF4F6256)
private val BeefMuted = Color(0xFF6B7D73)
private val BeefLine = Color(0xFFE3EAE6)

private val SoftBlue = Color(0xFFE8F2F5)
private val Blue = Color(0xFF397587)

private val SoftPurple = Color(0xFFF0EBF7)
private val Purple = Color(0xFF725B98)

private val SoftAmber = Color(0xFFFFF3D9)
private val Amber = Color(0xFFE39A18)

private val SoftRed = Color(0xFFFBE9E9)
private val Red = Color(0xFFB64A4A)


@Composable
fun DashboardTab(
    apiClient: ManagementApiClient,
    currentUserId: String,
    modifier: Modifier = Modifier
) {

    val dashboardViewModel: DashboardViewModel =
        viewModel(
            key = "dashboard-$currentUserId",
            factory =
                DashboardViewModelFactory(
                    apiClient
                )
        )

    LaunchedEffect(currentUserId) {
        dashboardViewModel.refresh()
    }

    DashboardScreen(
        viewModel = dashboardViewModel,
        modifier = modifier
    )
}


@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {

    val state by
        viewModel
            .uiState
            .collectAsState()

    Surface(
        modifier =
            modifier.fillMaxSize(),
        color =
            BeefBackground
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
        ) {

            DashboardHeader(
                summary = state.summary,
                loading = state.loading,
                onRefresh = viewModel::refresh
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 15.dp
                        )
            ) {

                Spacer(
                    Modifier.height(18.dp)
                )

                when {

                    state.needsConnection -> {

                        StatusNotice(
                            title = "You're offline",
                            message =
                                "Dashboard information will update when BeefTech reconnects.",
                            background =
                                SoftAmber,
                            foreground =
                                Color(0xFF74530D)
                        )

                        Spacer(
                            Modifier.height(14.dp)
                        )
                    }

                    !state.error.isNullOrBlank() -> {

                        StatusNotice(
                            title = "Dashboard unavailable",
                            message =
                                state.error
                                    ?: "Unable to load dashboard.",
                            background =
                                SoftRed,
                            foreground =
                                Red
                        )

                        Spacer(
                            Modifier.height(14.dp)
                        )
                    }
                }

                val summary =
                    state.summary

                if (
                    summary == null &&
                    state.loading
                ) {

                    LoadingDashboard()

                } else if (
                    summary != null
                ) {

                    Overview(
                        summary
                    )

                    Spacer(
                        Modifier.height(22.dp)
                    )

                    ActivityPanel(
                        summary
                    )

                    Spacer(
                        Modifier.height(22.dp)
                    )

                    TeamPanel(
                        summary
                    )

                    Spacer(
                        Modifier.height(22.dp)
                    )

                    AlertPanel(
                        summary
                    )
                }

                Spacer(
                    Modifier.height(28.dp)
                )

                BeefTechFooter()

                Spacer(
                    Modifier.height(28.dp)
                )
            }
        }
    }
}


@Composable
private fun DashboardHeader(
    summary: DashboardSummary?,
    loading: Boolean,
    onRefresh: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    BeefDarkGreen
                )
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = 22.dp,
                    bottom = 20.dp
                )
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = "BEEFTECH",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight =
                        FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(
                    Modifier.height(5.dp)
                )

                Text(
                    text = "Farm Dashboard",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(3.dp)
                )

                Text(
                    text =
                        "Data driven. Healthy herds.",
                    color =
                        Color.White.copy(
                            alpha = 0.72f
                        ),
                    fontSize = 12.sp
                )
            }

            Surface(
                shape = CircleShape,
                color =
                    Color.White.copy(
                        alpha = 0.14f
                    )
            ) {

                Box(
                    modifier =
                        Modifier.size(46.dp),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = "BT",
                        color = Color.White,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }

        Spacer(
            Modifier.height(18.dp)
        )

        Surface(
            shape =
                RoundedCornerShape(
                    16.dp
                ),
            color =
                Color.White.copy(
                    alpha = 0.10f
                )
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 13.dp,
                            vertical = 10.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(9.dp)
                            .clip(
                                CircleShape
                            )
                            .background(
                                if (
                                    summary != null
                                ) {
                                    Color(0xFFAEBBAF)
                                } else {
                                    Amber
                                }
                            )
                )

                Spacer(
                    Modifier.width(9.dp)
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            summary
                                ?.siteId
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Farm management",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Text(
                        text =
                            if (
                                summary != null
                            ) {
                                "Connected to management service"
                            } else {
                                "Waiting for farm data"
                            },
                        color =
                            Color.White.copy(
                                alpha = 0.64f
                            ),
                        fontSize = 10.sp
                    )
                }

                TextButton(
                    onClick = onRefresh,
                    enabled = !loading
                ) {

                    Text(
                        text =
                            if (loading) {
                                "..."
                            } else {
                                "Refresh"
                            },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}


@Composable
private fun Overview(
    summary: DashboardSummary
) {

    SectionTitle(
        smallTitle = "OVERVIEW",
        title = "Farm at a glance",
        subtitle =
            "Key records across your farm"
    )

    Spacer(
        Modifier.height(12.dp)
    )

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(
                10.dp
            )
    ) {

        MetricCard(
            modifier =
                Modifier.weight(1f),
            badge = "FR",
            title = "Farmers",
            value =
                summary
                    .farmers
                    .total
                    .toString(),
            detail =
                "${summary.farmers.last7Days} recent",
            accent = BeefGreen,
            softColor =
                BeefLightGreen
        )

        MetricCard(
            modifier =
                Modifier.weight(1f),
            badge = "CF",
            title = "Calves",
            value =
                summary
                    .calves
                    .total
                    .toString(),
            detail =
                "${summary.calves.last7Days} recent",
            accent =
                Color(0xFF667A6C),
            softColor =
                Color(0xFFEAF4ED)
        )
    }

    Spacer(
        Modifier.height(10.dp)
    )

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(
                10.dp
            )
    ) {

        MetricCard(
            modifier =
                Modifier.weight(1f),
            badge = "TR",
            title = "Treatments",
            value =
                summary
                    .treatments
                    .total
                    .toString(),
            detail =
                "${summary.treatments.last7Days} recent",
            accent = Blue,
            softColor = SoftBlue
        )

        MetricCard(
            modifier =
                Modifier.weight(1f),
            badge = "TM",
            title = "Team",
            value =
                summary
                    .team
                    .activeWorkers
                    .toString(),
            detail =
                if (
                    summary.team
                        .inactiveWorkers == 0L
                ) {
                    "All active"
                } else {
                    "${summary.team.inactiveWorkers} inactive"
                },
            accent = Purple,
            softColor = SoftPurple
        )
    }
}


@Composable
private fun MetricCard(
    modifier: Modifier,
    badge: String,
    title: String,
    value: String,
    detail: String,
    accent: Color,
    softColor: Color
) {

    Card(
        modifier = modifier,
        shape =
            RoundedCornerShape(
                19.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    BeefCard
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    2.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    14.dp
                )
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Surface(
                    shape =
                        RoundedCornerShape(
                            10.dp
                        ),
                    color = softColor
                ) {

                    Box(
                        modifier =
                            Modifier.size(
                                35.dp
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = badge,
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }

                Spacer(
                    Modifier.width(8.dp)
                )

                Text(
                    text = title,
                    color =
                        BeefMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }

            Spacer(
                Modifier.height(12.dp)
            )

            Text(
                text = value,
                color = BeefText,
                fontSize = 27.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text = detail,
                color = BeefMuted,
                fontSize = 9.sp
            )
        }
    }
}


@Composable
private fun ActivityPanel(
    summary: DashboardSummary
) {

    SectionTitle(
        smallTitle = "ACTIVITY",
        title = "Operational snapshot",
        subtitle =
            "Records received by BeefTech"
    )

    Spacer(
        Modifier.height(12.dp)
    )

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    BeefCard
            ),
        elevation =
            CardDefaults.cardElevation(
                2.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            ActivityLine(
                label =
                    "Farmer registrations",
                value =
                    summary.farmers.total,
                recent =
                    summary.farmers.last7Days
            )

            Line()

            ActivityLine(
                label =
                    "Calf registrations",
                value =
                    summary.calves.total,
                recent =
                    summary.calves.last7Days
            )

            Line()

            ActivityLine(
                label = "Treatments",
                value =
                    summary.treatments.total,
                recent =
                    summary.treatments.last7Days
            )

            Line()

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 11.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Treatment value",
                        color = BeefText,
                        fontWeight =
                            FontWeight.SemiBold,
                        fontSize = 12.sp
                    )

                    Text(
                        text =
                            "Recorded treatment cost",
                        color = BeefMuted,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text =
                        zar(
                            summary
                                .treatments
                                .totalCost
                        ),
                    color = BeefGreen,
                    fontWeight =
                        FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}


@Composable
private fun ActivityLine(
    label: String,
    value: Long,
    recent: Long
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 11.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text = label,
                color = BeefText,
                fontSize = 12.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text =
                    "$recent in the last 7 days",
                color = BeefMuted,
                fontSize = 10.sp
            )
        }

        Surface(
            shape =
                RoundedCornerShape(
                    11.dp
                ),
            color = BeefMint
        ) {

            Text(
                text =
                    value.toString(),
                modifier =
                    Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    ),
                color = BeefGreen,
                fontWeight =
                    FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}


@Composable
private fun TeamPanel(
    summary: DashboardSummary
) {

    SectionTitle(
        smallTitle = "TEAM",
        title = "Farm workforce",
        subtitle =
            "Current worker status"
    )

    Spacer(
        Modifier.height(12.dp)
    )

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    BeefDeepGreen
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    17.dp
                )
        ) {

            Text(
                text =
                    "People keeping the farm moving",
                color =
                    Color.White.copy(
                        alpha = 0.65f
                    ),
                fontSize = 10.sp
            )

            Spacer(
                Modifier.height(13.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                TeamStat(
                    modifier =
                        Modifier.weight(1f),
                    title =
                        "Active workers",
                    number =
                        summary
                            .team
                            .activeWorkers,
                    dot =
                        Color(0xFFAEBBAF)
                )

                TeamStat(
                    modifier =
                        Modifier.weight(1f),
                    title =
                        "Inactive",
                    number =
                        summary
                            .team
                            .inactiveWorkers,
                    dot =
                        Color(0xFFF0BB57)
                )
            }
        }
    }
}


@Composable
private fun TeamStat(
    modifier: Modifier,
    title: String,
    number: Long,
    dot: Color
) {

    Surface(
        modifier = modifier,
        shape =
            RoundedCornerShape(
                15.dp
            ),
        color =
            Color.White.copy(
                alpha = 0.09f
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    13.dp
                )
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(dot)
                )

                Spacer(
                    Modifier.width(6.dp)
                )

                Text(
                    text = title,
                    color =
                        Color.White.copy(
                            alpha = 0.70f
                        ),
                    fontSize = 10.sp
                )
            }

            Spacer(
                Modifier.height(7.dp)
            )

            Text(
                text =
                    number.toString(),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


@Composable
private fun AlertPanel(
    summary: DashboardSummary
) {

    SectionTitle(
        smallTitle = "MONITORING",
        title = "Alerts",
        subtitle =
            "Anything that needs attention"
    )

    Spacer(
        Modifier.height(12.dp)
    )

    if (
        summary.alerts.isEmpty()
    ) {

        Surface(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(
                    18.dp
                ),
            color =
                BeefLightGreen
        ) {

            Row(
                modifier =
                    Modifier.padding(
                        15.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Surface(
                    shape = CircleShape,
                    color = BeefGreen
                ) {

                    Box(
                        modifier =
                            Modifier.size(
                                36.dp
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = "✓",
                            color =
                                Color.White,
                            fontSize = 16.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }

                Spacer(
                    Modifier.width(11.dp)
                )

                Column {

                    Text(
                        text =
                            "Everything looks good",
                        color =
                            BeefDarkGreen,
                        fontSize = 12.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "There are no management alerts right now.",
                        color =
                            BeefMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }

    } else {

        Column(
            verticalArrangement =
                Arrangement.spacedBy(
                    9.dp
                )
        ) {

            summary.alerts.forEach {
                    alert ->

                Surface(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(
                            17.dp
                        ),
                    color =
                        SoftAmber
                ) {

                    Row(
                        modifier =
                            Modifier.padding(
                                14.dp
                            ),
                        verticalAlignment =
                            Alignment.Top
                    ) {

                        Surface(
                            shape =
                                CircleShape,
                            color = Amber
                        ) {

                            Box(
                                modifier =
                                    Modifier.size(
                                        32.dp
                                    ),
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Text(
                                    text = "!",
                                    color =
                                        Color.White,
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }

                        Spacer(
                            Modifier.width(
                                10.dp
                            )
                        )

                        Text(
                            text =
                                alert.message,
                            modifier =
                                Modifier.weight(
                                    1f
                                ),
                            color =
                                Color(
                                    0xFF73520A
                                ),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun SectionTitle(
    smallTitle: String,
    title: String,
    subtitle: String
) {

    Column {

        Text(
            text = smallTitle,
            color = BeefGreen,
            fontSize = 9.sp,
            letterSpacing = 1.2.sp,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            Modifier.height(2.dp)
        )

        Text(
            text = title,
            color = BeefText,
            fontSize = 18.sp,
            fontWeight =
                FontWeight.Bold
        )

        Text(
            text = subtitle,
            color = BeefMuted,
            fontSize = 10.sp
        )
    }
}


@Composable
private fun StatusNotice(
    title: String,
    message: String,
    background: Color,
    foreground: Color
) {

    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                16.dp
            ),
        color = background
    ) {

        Column(
            modifier =
                Modifier.padding(
                    14.dp
                )
        ) {

            Text(
                text = title,
                color = foreground,
                fontWeight =
                    FontWeight.Bold,
                fontSize = 12.sp
            )

            Text(
                text = message,
                color =
                    foreground.copy(
                        alpha = 0.78f
                    ),
                fontSize = 10.sp
            )
        }
    }
}


@Composable
private fun LoadingDashboard() {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 60.dp
                ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        CircularProgressIndicator(
            color = BeefGreen,
            strokeWidth = 3.dp
        )

        Spacer(
            Modifier.height(13.dp)
        )

        Text(
            text =
                "Loading farm dashboard...",
            color = BeefMuted,
            fontSize = 11.sp
        )
    }
}


@Composable
private fun Line() {

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    BeefLine
                )
    )
}


@Composable
private fun BeefTechFooter() {

    Column(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = "BEEFTECH",
            color = BeefDarkGreen,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            fontWeight =
                FontWeight.Bold
        )

        Text(
            text =
                "Data driven. Healthy herds. Stronger farms.",
            color = BeefMuted,
            fontSize = 8.sp
        )
    }
}


private fun zar(
    value: Double
): String {

    return try {

        NumberFormat
            .getCurrencyInstance(
                Locale(
                    "en",
                    "ZA"
                )
            )
            .format(value)

    } catch (_: Exception) {

        "R %.2f".format(
            value
        )
    }
}
