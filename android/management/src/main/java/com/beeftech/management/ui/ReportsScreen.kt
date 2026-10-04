package com.beeftech.management.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.ReportData
import com.beeftech.management.data.ReportFormat
import com.beeftech.management.data.ReportKind
import com.beeftech.management.viewmodel.REPORT_BUCKETS
import com.beeftech.management.viewmodel.REPORT_RANGES
import com.beeftech.management.viewmodel.ReportsViewModel
import com.beeftech.management.viewmodel.ReportsViewModelFactory

/* Keyed by user, so a different user logging in on the same device never sees the previous report. */
@Composable
fun ReportsTab(
    apiClient: ManagementApiClient,
    currentUserId: String,
    isAdmin: Boolean = false,
    modifier: Modifier = Modifier
) {
    val viewModel: ReportsViewModel = viewModel(
        key = "reports-$currentUserId",
        factory = ReportsViewModelFactory(apiClient, canSwitchSite = isAdmin)
    )

    ReportsScreen(viewModel = viewModel, modifier = modifier)
}

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.refresh() }

    /* Hand the exported file to the share sheet exactly once. */
    LaunchedEffect(state.exported) {
        state.exported?.let {
            context.startActivity(ReportShare.shareIntent(context, it))
            viewModel.exportHandled()
        }
    }

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
            Text(
                state.report?.siteName?.let { "Reports · $it" } ?: "Reports",
                style = MaterialTheme.typography.titleLarge
            )
            TextButton(onClick = viewModel::refresh) { Text("Refresh") }
        }

        ChipRow {
            ReportKind.entries.forEach {
                FilterChip(
                    selected = state.kind == it,
                    onClick = { viewModel.selectKind(it) },
                    label = { Text(it.label) }
                )
            }
        }

        ChipRow {
            REPORT_RANGES.forEach {
                FilterChip(
                    selected = state.rangeDays == it,
                    onClick = { viewModel.selectRange(it) },
                    label = { Text("Last $it days") }
                )
            }
        }

        if (state.kind == ReportKind.CALF_REGISTRATIONS) {
            ChipRow {
                REPORT_BUCKETS.forEach {
                    FilterChip(
                        selected = state.bucket == it,
                        onClick = { viewModel.selectBucket(it) },
                        label = { Text("By $it") }
                    )
                }
            }
        }

        if (state.sites.isNotEmpty()) {
            ChipRow {
                FilterChip(
                    selected = state.selectedSiteId == null,
                    onClick = { viewModel.selectSite(null) },
                    label = { Text("All sites") }
                )
                state.sites.forEach { site ->
                    FilterChip(
                        selected = state.selectedSiteId == site.siteId,
                        onClick = { viewModel.selectSite(site.siteId) },
                        label = { Text(site.name) }
                    )
                }
            }
        }

        if (state.needsConnection) {
            NeedsConnectionNotice("Reports")
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        val report = state.report
        if (report != null) {
            ReportBody(report)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.export(ReportFormat.CSV) },
                    enabled = !state.exporting
                ) { Text("Share CSV") }
                OutlinedButton(
                    onClick = { viewModel.export(ReportFormat.PDF) },
                    enabled = !state.exporting
                ) { Text("Share PDF") }
            }
            if (state.exporting) CircularProgressIndicator()
        } else if (state.loading) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun ReportBody(report: ReportData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(report.title, style = MaterialTheme.typography.labelLarge)
            report.summary.forEach {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(it.label, style = MaterialTheme.typography.bodyMedium)
                    Text(it.value, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    if (report.rows.isEmpty()) {
        Text("No records in this period.")
    } else {
        Column(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TableRow(report.columns, header = true)
            HorizontalDivider()
            report.rows.forEach { TableRow(it, header = false) }
        }
    }

    report.footer?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun TableRow(cells: List<String>, header: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        cells.forEachIndexed { index, cell ->
            Text(
                cell,
                modifier = Modifier.widthIn(min = if (index == 0) 140.dp else 72.dp),
                style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium
            )
        }
    }
}
