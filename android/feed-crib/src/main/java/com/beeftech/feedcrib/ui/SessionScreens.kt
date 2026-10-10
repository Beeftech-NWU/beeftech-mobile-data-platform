package com.beeftech.feedcrib.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeftech.database.dao.FeedCribSessionRow
import com.beeftech.database.entity.FeedCribEntryEntity
import com.beeftech.feedcrib.data.SlotAllocator
import java.text.DateFormat
import java.util.Date

/** One line per crib touched today: M / D / E codes, the day's ADI and whether it has been sent. */
@Composable
fun SessionListScreen(
    sessions: List<FeedCribSessionRow>,
    onOpen: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(FeedCribColors.LightBg)) {
        FeedHeader(
            eyebrow = "Feed crib",
            title = "Today's session",
            subtitle = if (sessions.size == 1) "1 crib read today." else "${sessions.size} cribs read today.",
            icon = Icons.Outlined.ListAlt,
            onBackClick = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (sessions.isEmpty()) {
                item { Text("No readings yet today.", color = FeedCribColors.MutedText, fontSize = 13.sp) }
            }
            items(sessions, key = { it.cribNumber }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FeedCribColors.LightSurface, RoundedCornerShape(12.dp))
                        .clickable { onOpen(row.cribNumber) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            row.cribNumber,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = FeedCribColors.DarkText
                        )
                        Text(
                            "M ${row.morningCode ?: "–"} · D ${row.midDayCode ?: "–"} · E ${row.eveningCode ?: "–"}  ·  ADI ${formatKg(row.latestAdi)}",
                            fontSize = 12.sp,
                            color = FeedCribColors.MutedText
                        )
                    }
                    if (row.unsyncedCount > 0) {
                        FeedPill("Waiting", FeedCribColors.Waiting, FeedCribColors.WaitingBg)
                    } else {
                        FeedPill("Synced", FeedCribColors.Synced, FeedCribColors.SyncedBg)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("›", fontSize = 24.sp, color = FeedCribColors.RustDeep)
                }
            }
        }
    }
}

/** Read-only: every reading saved on one crib today, newest first, with its block, ADI and sync status. */
@Composable
fun SessionDetailScreen(
    cribNumber: String,
    entries: List<FeedCribEntryEntity>,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(FeedCribColors.LightBg)) {
        FeedHeader(
            eyebrow = "Today's readings",
            title = cribNumber,
            subtitle = if (entries.size == 1) "1 reading saved." else "${entries.size} readings saved.",
            icon = Icons.Outlined.ListAlt,
            onBackClick = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(entries, key = { it.recordGuid }) { entry ->
                FeedCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(entry.capturedAt)),
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = FeedCribColors.DarkText
                        )
                        SyncStatusPill(entry.syncStatus)
                    }
                    Spacer(Modifier.padding(top = 6.dp))
                    FeedDetailRow("Block", SlotAllocator.label(entry.slot))
                    FeedDetailRow("Code", entry.code?.toString() ?: "No code (ADI only)")
                    FeedDetailRow("A.D.I", formatKg(entry.adi))
                    if (!entry.syncError.isNullOrBlank() && entry.syncStatus != "SYNCED") {
                        Text(entry.syncError!!, color = FeedCribColors.Refused, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
