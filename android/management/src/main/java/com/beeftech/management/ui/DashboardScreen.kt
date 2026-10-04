package com.beeftech.management.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beeftech.management.data.DashboardSummary
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.viewmodel.DashboardViewModel
import com.beeftech.management.viewmodel.DashboardViewModelFactory
import java.util.Locale

private val DashboardSage =
    Color(
        0xFF4F6256
    )

private val DashboardAccent =
    Color(
        0xFF667A6C
    )

private val DashboardBackground =
    Color(
        0xFFFAF9F2
    )

private val DashboardCard =
    Color.White

private val DashboardSoftGreen =
    Color(
        0xFFE3E8E2
    )

private val DashboardSoftAmber =
    Color(
        0xFFF3E9DD
    )

private val DashboardDanger =
    Color(
        0xFF8A4F4F
    )


@Composable
fun DashboardTab(
    apiClient: ManagementApiClient,
    currentUserId: String,
    isAdmin: Boolean = false,
    modifier: Modifier = Modifier
) {

    val viewModel:
            DashboardViewModel =
        viewModel(
            key =
                "dashboard-$currentUserId",

            factory =
                DashboardViewModelFactory(
                    apiClient,
                    canSwitchSite =
                        isAdmin
                )
        )


    DashboardScreen(
        viewModel =
            viewModel,
        modifier =
            modifier
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


    LaunchedEffect(
        Unit
    ) {

        viewModel.refresh()
    }


    Surface(
        modifier =
            modifier
                .fillMaxSize(),
        color =
            DashboardBackground
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
                summary =
                    state.summary,
                loading =
                    state.loading,
                onRefresh =
                    viewModel::refresh
            )


            Column(
                modifier =
                    Modifier.padding(
                        16.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        14.dp
                    )
            ) {

                if (
                    state.sites
                        .isNotEmpty()
                ) {

                    Text(
                        text =
                            "VIEW SITE",
                        style =
                            MaterialTheme
                                .typography
                                .labelSmall,
                        color =
                            DashboardAccent
                    )


                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(
                                    rememberScrollState()
                                ),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        FilterChip(
                            selected =
                                state.selectedSiteId ==
                                    null,
                            onClick = {

                                viewModel
                                    .selectSite(
                                        null
                                    )
                            },
                            label = {

                                Text(
                                    "All sites"
                                )
                            }
                        )


                        state.sites
                            .forEach {
                                    site ->

                                FilterChip(
                                    selected =
                                        state.selectedSiteId ==
                                            site.siteId,

                                    onClick = {

                                        viewModel
                                            .selectSite(
                                                site.siteId
                                            )
                                    },

                                    label = {

                                        Text(
                                            site.name
                                        )
                                    }
                                )
                            }
                    }
                }


                if (
                    state.needsConnection
                ) {

                    NeedsConnectionNotice(
                        "The dashboard"
                    )
                }


                state.error
                    ?.let {
                            error ->

                        StatusCard(
                            title =
                                "Dashboard unavailable",

                            message =
                                error,

                            background =
                                Color(
                                    0xFFF4E3E1
                                ),

                            foreground =
                                DashboardDanger
                        )
                    }


                val summary =
                    state.summary


                if (
                    summary !=
                    null
                ) {

                    Text(
                        text =
                            "FARM OVERVIEW",
                        style =
                            MaterialTheme
                                .typography
                                .labelSmall,
                        color =
                            DashboardAccent
                    )


                    Text(
                        text =
                            "Operational snapshot",
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,
                        fontWeight =
                            FontWeight.SemiBold
                    )


                    MetricGrid(
                        summary =
                            summary
                    )


                    CostPanel(
                        summary =
                            summary
                    )


                    TeamPanel(
                        summary =
                            summary
                    )


                    AlertsPanel(
                        summary =
                            summary
                    )

                } else if (
                    state.loading
                ) {

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth(),
                        shape =
                            RoundedCornerShape(
                                16.dp
                            ),
                        colors =
                            CardDefaults
                                .cardColors(
                                    containerColor =
                                        DashboardCard
                                )
                    ) {

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        28.dp
                                    ),
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    10.dp
                                )
                        ) {

                            CircularProgressIndicator(
                                color =
                                    DashboardSage
                            )


                            Text(
                                "Loading dashboard?"
                            )
                        }
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            14.dp
                        )
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
                    DashboardSage
                )
                .padding(
                    horizontal = 18.dp,
                    vertical = 20.dp
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
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    text =
                        "BEEFTECH",
                    color =
                        Color.White
                            .copy(
                                alpha = 0.72f
                            ),
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall
                )


                Text(
                    text =
                        "Farm Dashboard",
                    color =
                        Color.White,
                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall,
                    fontWeight =
                        FontWeight.SemiBold
                )


                Text(
                    text =
                        summary
                            ?.siteName
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: summary
                                ?.siteId
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                            ?: "Farm management",

                    color =
                        Color.White
                            .copy(
                                alpha = 0.80f
                            ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }


            Surface(
                shape =
                    CircleShape,
                color =
                    Color.White
                        .copy(
                            alpha = 0.14f
                        )
            ) {

                Box(
                    modifier =
                        Modifier.size(
                            44.dp
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            "BT",
                        color =
                            Color.White,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )


        TextButton(
            onClick =
                onRefresh,
            enabled =
                !loading
        ) {

            Text(
                text =
                    if (
                        loading
                    ) {

                        "Refreshing?"

                    } else {

                        "Refresh dashboard"
                    },
                color =
                    Color.White
            )
        }
    }
}


private data class DashboardMetric(
    val title: String,
    val headline: String,
    val detail: String
)


@Composable
private fun MetricGrid(
    summary: DashboardSummary
) {

    val metrics =
        buildList {

            add(
                DashboardMetric(
                    title =
                        "Farmers",
                    headline =
                        "${summary.farmers.total}",
                    detail =
                        "${summary.farmers.last7Days} recent"
                )
            )


            add(
                DashboardMetric(
                    title =
                        "Calf registrations",
                    headline =
                        "${summary.calves.total}",
                    detail =
                        "${summary.calves.last7Days} in 7 days"
                )
            )


            add(
                DashboardMetric(
                    title =
                        "Treatments",
                    headline =
                        "${summary.treatments.total}",
                    detail =
                        "${summary.treatments.last7Days} in 7 days"
                )
            )


            add(
                DashboardMetric(
                    title =
                        "Team",
                    headline =
                        "${summary.team.activeWorkers}",
                    detail =
                        "${summary.team.inactiveWorkers} inactive"
                )
            )


            summary.mortalities
                ?.let {
                        value ->

                    add(
                        DashboardMetric(
                            title =
                                "Mortalities",
                            headline =
                                "${value.total}",
                            detail =
                                "${value.last7Days} in 7 days"
                        )
                    )
                }


            summary.movements
                ?.let {
                        value ->

                    add(
                        DashboardMetric(
                            title =
                                "Movements",
                            headline =
                                "${value.total}",
                            detail =
                                "${value.last7Days} in 7 days"
                        )
                    )
                }


            summary.costs
                ?.let {
                        value ->

                    add(
                        DashboardMetric(
                            title =
                                "Other costs",
                            headline =
                                "${value.total}",
                            detail =
                                "R ${formatMoney(value.totalAmount)}"
                        )
                    )
                }


            summary.feedReadings
                ?.let {
                        value ->

                    add(
                        DashboardMetric(
                            title =
                                "Feed readings",
                            headline =
                                "${value.total}",
                            detail =
                                "${value.last7Days} in 7 days"
                        )
                    )
                }
        }


    metrics
        .chunked(
            2
        )
        .forEach {
                rowMetrics ->

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                rowMetrics
                    .forEach {
                            metric ->

                        MetricCard(
                            metric =
                                metric,
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )
                    }


                if (
                    rowMetrics.size ==
                    1
                ) {

                    Spacer(
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )
        }
}


@Composable
private fun MetricCard(
    metric: DashboardMetric,
    modifier: Modifier
) {

    Card(
        modifier =
            modifier,
        shape =
            RoundedCornerShape(
                16.dp
            ),
        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        DashboardCard
                )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    15.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    4.dp
                )
        ) {

            Text(
                text =
                    metric.title,
                style =
                    MaterialTheme
                        .typography
                        .labelMedium,
                color =
                    DashboardAccent
            )


            Text(
                text =
                    metric.headline,
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,
                fontWeight =
                    FontWeight.Bold,
                color =
                    DashboardSage
            )


            Text(
                text =
                    metric.detail,
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}


@Composable
private fun CostPanel(
    summary: DashboardSummary
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                16.dp
            ),
        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        DashboardSoftGreen
                )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {

            Text(
                text =
                    "COST OVERVIEW",
                style =
                    MaterialTheme
                        .typography
                        .labelSmall,
                color =
                    DashboardAccent
            )


            Text(
                text =
                    "Treatment value",
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                fontWeight =
                    FontWeight.SemiBold
            )


            Text(
                text =
                    "R ${
                        formatMoney(
                            summary
                                .treatments
                                .totalCost
                        )
                    }",
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,
                fontWeight =
                    FontWeight.Bold,
                color =
                    DashboardSage
            )


            summary.costs
                ?.let {
                        costs ->

                    Text(
                        text =
                            "Other recorded costs: R ${
                                formatMoney(
                                    costs.totalAmount
                                )
                            }",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }
        }
    }
}


@Composable
private fun TeamPanel(
    summary: DashboardSummary
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                16.dp
            ),
        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        DashboardSage
                )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    6.dp
                )
        ) {

            Text(
                text =
                    "TEAM STATUS",
                style =
                    MaterialTheme
                        .typography
                        .labelSmall,
                color =
                    Color.White
                        .copy(
                            alpha = 0.72f
                        )
            )


            Text(
                text =
                    "${summary.team.activeWorkers} active workers",
                style =
                    MaterialTheme
                        .typography
                        .titleLarge,
                color =
                    Color.White,
                fontWeight =
                    FontWeight.SemiBold
            )


            Text(
                text =
                    "${summary.team.inactiveWorkers} deactivated",
                color =
                    Color.White
                        .copy(
                            alpha = 0.74f
                        ),
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }
    }
}


@Composable
private fun AlertsPanel(
    summary: DashboardSummary
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                16.dp
            ),
        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        if (
                            summary.alerts
                                .isEmpty()
                        ) {

                            DashboardSoftGreen

                        } else {

                            DashboardSoftAmber
                        }
                )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    7.dp
                )
        ) {

            Text(
                text =
                    "Alerts",
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                fontWeight =
                    FontWeight.SemiBold
            )


            if (
                summary.alerts
                    .isEmpty()
            ) {

                Text(
                    text =
                        "No alerts.",
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )

            } else {

                summary.alerts
                    .forEach {
                            alert ->

                        Text(
                            text =
                                "? ${alert.message}",
                            color =
                                DashboardDanger,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }
            }
        }
    }
}


@Composable
private fun StatusCard(
    title: String,
    message: String,
    background: Color,
    foreground: Color
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                14.dp
            ),
        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        background
                )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    14.dp
                )
        ) {

            Text(
                text =
                    title,
                fontWeight =
                    FontWeight.SemiBold,
                color =
                    foreground
            )


            Text(
                text =
                    message,
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    foreground
            )
        }
    }
}


fun formatMoney(
    amount: Double
): String =
    String.format(
        Locale.US,
        "%,.2f",
        amount
    )
