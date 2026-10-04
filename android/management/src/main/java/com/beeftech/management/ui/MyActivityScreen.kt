package com.beeftech.management.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beeftech.management.data.roleLabel
import java.text.DateFormat
import java.util.Date

/**
 * What a worker can see about their own account and sync backlog. Works offline:
 * it takes plain values, so it needs neither the backend nor a ViewModel.
 */
@Composable
fun MyActivityScreen(
    username: String,
    role: Int?,
    siteId: String?,
    pendingCount: Int,
    oldestPendingAt: Long?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("My activity", style = MaterialTheme.typography.titleLarge)
        Text("Signed in as $username (${roleLabel(role)})")
        Text("Site: ${siteId ?: "none"}")
        Text(syncSummary(pendingCount, oldestPendingAt))
    }
}

fun syncSummary(pendingCount: Int, oldestPendingAt: Long?): String =
    when {
        pendingCount == 0 -> "Everything is synced."
        oldestPendingAt == null -> "$pendingCount ${recordWord(pendingCount)} waiting to sync."
        else ->
            "$pendingCount ${recordWord(pendingCount)} waiting to sync, " +
                "oldest from ${DateFormat.getDateTimeInstance().format(Date(oldestPendingAt))}."
    }

private fun recordWord(count: Int) = if (count == 1) "record" else "records"
