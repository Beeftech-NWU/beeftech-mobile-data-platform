package com.beeftech.management.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beeftech.management.data.DashboardSummary
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.viewmodel.DashboardViewModel
import com.beeftech.management.viewmodel.DashboardViewModelFactory
import java.util.Locale

/* Keyed by user, so a different user logging in on the same device never sees the previous numbers. */
@Composable
fun DashboardTab(
    apiClient: ManagementApiClient,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    val viewModel: DashboardViewModel = viewModel(
        key = "dashboard-$currentUserId",
        factory = DashboardViewModelFactory(apiClient)
    )

    DashboardScreen(viewModel = viewModel, modifier = modifier)
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dashboard", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = viewModel::refresh) { Text("Refresh") }
        }

        if (state.needsConnection) {
            Text(
                "The dashboard needs a connection. Check your signal and tap Refresh.",
                color = MaterialTheme.colorScheme.error
            )
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        val summary = state.summary
        if (summary != null) {
            SummaryCards(summary)
        } else if (state.loading) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun SummaryCards(summary: DashboardSummary) {
    StatCard(
        "Calf registrations",
        "${summary.calves.total} total",
        "${summary.calves.last7Days} in the last 7 days"
    )
    StatCard(
        "Treatments",
        "${summary.treatments.total} total · ${formatMoney(summary.treatments.totalCost)}",
        "${summary.treatments.last7Days} in the last 7 days"
    )
    StatCard(
        "Farmers",
        "${summary.farmers.total} registered",
        "${summary.farmers.last7Days} reached the server in the last 7 days"
    )
    summary.mortalities?.let {
        StatCard("Mortalities", "${it.total} total", "${it.last7Days} in the last 7 days")
    }
    summary.movements?.let {
        StatCard("Movements", "${it.total} total", "${it.last7Days} in the last 7 days")
    }
    summary.costs?.let {
        StatCard(
            "Other costs",
            "${it.total} total · ${formatMoney(it.totalAmount)}",
            "${it.last7Days} in the last 7 days, not counting treatment costs"
        )
    }
    summary.feedReadings?.let {
        StatCard("Feed readings", "${it.total} total", "${it.last7Days} in the last 7 days")
    }
    StatCard(
        "Team",
        "${summary.team.activeWorkers} active workers",
        "${summary.team.inactiveWorkers} deactivated"
    )

    Text("Alerts", style = MaterialTheme.typography.titleMedium)
    if (summary.alerts.isEmpty()) {
        Text("No alerts.")
    } else {
        summary.alerts.forEach {
            Text(it.message, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun StatCard(title: String, headline: String, detail: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(headline, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun formatMoney(amount: Double): String = String.format(Locale.US, "%,.2f", amount)
