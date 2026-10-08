package com.beeftech.management.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beeftech.database.entity.SyncRunEntity
import com.beeftech.database.entity.SyncRunResult
import com.beeftech.database.util.SyncRunDisplay
import com.beeftech.management.data.roleLabel
import java.text.DateFormat
import java.util.Date

private val ManagementSage =
    Color(
        0xFF4F6256
    )

private val ManagementAccent =
    Color(
        0xFF667A6C
    )

private val ManagementBackground =
    Color(
        0xFFFAF9F2
    )

private val ManagementCard =
    Color(
        0xFFF4F3E8
    )

private val ManagementSoftGreen =
    Color(
        0xFFE3E8E2
    )

private val ManagementSuccess =
    Color(
        0xFF3F6A50
    )

private val ManagementWarning =
    Color(
        0xFF8A6542
    )


/**
 * What a worker can see about their own account and sync backlog.
 *
 * Works offline because all values are supplied by MainActivity.
 */
@Composable
fun MyActivityScreen(
    username: String,
    role: Int?,
    siteId: String?,
    pendingCount: Int,
    oldestPendingAt: Long?,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    syncHistory: List<SyncRunEntity> = emptyList()
) {

    val isSynced =
        pendingCount == 0


    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    ManagementBackground
                )
                .verticalScroll(
                    rememberScrollState()
                )
    ) {

        /*
         * Page header
         */
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        ManagementSage
                    )
                    .padding(
                        horizontal = 18.dp,
                        vertical = 16.dp
                    )
        ) {

            TextButton(
                onClick =
                    onBack
            ) {

                Text(
                    text =
                        "‹  Back",
                    color =
                        Color.White,
                    style =
                        MaterialTheme
                            .typography
                            .labelLarge
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )


            Text(
                text =
                    "MY ACCOUNT",
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


            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )


            Text(
                text =
                    "My activity",
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
                    "Account and synchronization overview",
                color =
                    Color.White
                        .copy(
                            alpha = 0.82f
                        ),
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }


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

            /*
             * Profile card
             */
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
                                ManagementCard
                        )
            ) {

                Column(
                    modifier =
                        Modifier.padding(
                            18.dp
                        )
                ) {

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
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
                                    "Signed in as",
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelMedium,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )


                            Text(
                                text =
                                    username,
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleLarge,
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }


                        Surface(
                            color =
                                ManagementSoftGreen,
                            shape =
                                RoundedCornerShape(
                                    50.dp
                                )
                        ) {

                            Text(
                                text =
                                    roleLabel(
                                        role
                                    ),
                                modifier =
                                    Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 7.dp
                                    ),
                                color =
                                    ManagementSage,
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelMedium,
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }
                    }


                    Spacer(
                        modifier =
                            Modifier.height(
                                16.dp
                            )
                    )


                    HorizontalDivider(
                        color =
                            ManagementSage
                                .copy(
                                    alpha = 0.12f
                                )
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                14.dp
                            )
                    )


                    Text(
                        text =
                            "SITE",
                        style =
                            MaterialTheme
                                .typography
                                .labelSmall,
                        color =
                            ManagementAccent
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                3.dp
                            )
                    )


                    Text(
                        text =
                            siteId
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "No site assigned",
                        style =
                            MaterialTheme
                                .typography
                                .bodyLarge,
                        fontWeight =
                            FontWeight.Medium
                    )
                }
            }


            /*
             * Sync card
             */
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
                                if (
                                    isSynced
                                ) {

                                    ManagementSoftGreen

                                } else {

                                    Color(
                                        0xFFF3E9DD
                                    )
                                }
                        )
            ) {

                Column(
                    modifier =
                        Modifier.padding(
                            18.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column {

                            Text(
                                text =
                                    "SYNC STATUS",
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelSmall,
                                color =
                                    if (
                                        isSynced
                                    ) {

                                        ManagementSuccess

                                    } else {

                                        ManagementWarning
                                    }
                            )


                            Text(
                                text =
                                    if (
                                        isSynced
                                    ) {

                                        "Up to date"

                                    } else {

                                        "Sync pending"
                                    },
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }


                        Surface(
                            color =
                                Color.White
                                    .copy(
                                        alpha = 0.72f
                                    ),
                            shape =
                                RoundedCornerShape(
                                    12.dp
                                )
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(
                                        horizontal = 14.dp,
                                        vertical = 8.dp
                                    ),
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                Text(
                                    text =
                                        pendingCount
                                            .toString(),
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleLarge,
                                    fontWeight =
                                        FontWeight.Bold,
                                    color =
                                        if (
                                            isSynced
                                        ) {

                                            ManagementSuccess

                                        } else {

                                            ManagementWarning
                                        }
                                )


                                Text(
                                    text =
                                        if (
                                            pendingCount == 1
                                        ) {

                                            "pending"

                                        } else {

                                            "pending"
                                        },
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelSmall
                                )
                            }
                        }
                    }


                    HorizontalDivider(
                        color =
                            ManagementSage
                                .copy(
                                    alpha = 0.12f
                                )
                    )


                    Text(
                        text =
                            syncSummary(
                                pendingCount,
                                oldestPendingAt
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }


            /*
             * Sync history: the last runs, newest first.
             */
            SyncHistoryCard(syncHistory)


            /*
             * Information card
             */
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
                                Color.White
                        )
            ) {

                Column(
                    modifier =
                        Modifier.padding(
                            18.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            6.dp
                        )
                ) {

                    Text(
                        text =
                            "About synchronization",
                        style =
                            MaterialTheme
                                .typography
                                .titleSmall,
                        fontWeight =
                            FontWeight.SemiBold
                    )


                    Text(
                        text =
                            "Records created while offline stay on this device and synchronize when a connection is available.",
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
    }
}


@Composable
private fun SyncHistoryCard(runs: List<SyncRunEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Sync history",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            if (runs.isEmpty()) {
                Text(
                    text = "No sync has run on this device for you yet. Runs are kept for 30 days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            runs.forEachIndexed { index, run ->
                if (index > 0) HorizontalDivider(color = ManagementSage.copy(alpha = 0.12f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = SyncRunDisplay.moduleLabel(run.module),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${SyncRunDisplay.triggerLabel(run.trigger)} · " +
                                SyncRunDisplay.timeLabel(run.startedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = SyncRunDisplay.countsLine(run),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        run.message?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Text(
                        text = SyncRunDisplay.resultLabel(run.result),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (run.result == SyncRunResult.SUCCESS) ManagementSage else ManagementWarning
                    )
                }
            }
        }
    }
}


fun syncSummary(
    pendingCount: Int,
    oldestPendingAt: Long?
): String =
    when {

        pendingCount == 0 ->

            "Everything is synced."


        oldestPendingAt == null ->

            "$pendingCount ${recordWord(pendingCount)} waiting to sync."


        else ->

            "$pendingCount ${recordWord(pendingCount)} waiting to sync, " +
                "oldest from ${
                    DateFormat
                        .getDateTimeInstance()
                        .format(
                            Date(
                                oldestPendingAt
                            )
                        )
                }."
    }


private fun recordWord(
    count: Int
): String =
    if (
        count == 1
    ) {

        "record"

    } else {

        "records"
    }
