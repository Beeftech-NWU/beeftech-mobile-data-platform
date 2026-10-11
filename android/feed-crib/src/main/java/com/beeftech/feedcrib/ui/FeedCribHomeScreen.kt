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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeftech.database.entity.FeedCribEntity
import java.text.DateFormat
import java.util.Date

/** Crib entry: type a crib number and Go, or pick one from the site's list. */
@Composable
fun FeedCribHomeScreen(
    cribs: List<FeedCribEntity>,
    lastDownloadedAt: Long?,
    refreshing: Boolean,
    errorMessage: String?,
    onOpenCrib: (String) -> Unit,
    onSessions: () -> Unit,
    onSync: () -> Unit,
    onRefresh: () -> Unit,
    syncing: Boolean = false,
    syncMessage: String? = null,
    onResolveConflict: ((String) -> Unit)? = null
) {
    var number by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }

    val visible = remember(search, cribs) {
        val term = search.trim()
        cribs.filter {
            term.isEmpty() ||
                it.cribNumber.contains(term, ignoreCase = true) ||
                it.penDescription.contains(term, ignoreCase = true) ||
                it.description.contains(term, ignoreCase = true)
        }
    }

    val headerState = rememberCollapsingHeaderState()

    Column(modifier = Modifier.fillMaxSize().background(FeedCribColors.LightBg)) {
        FeedHeader(
            eyebrow = "BeefTech",
            title = "Feed Crib",
            subtitle = "Enter a crib number to take a reading, or pick one from the list.",
            icon = Icons.Outlined.Restaurant,
            modifier = Modifier.collapsingHeader(headerState)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().nestedScroll(headerState.nestedScrollConnection),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                FeedCard {
                    FeedSectionTitle("Crib number")
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = number,
                            onValueChange = { number = it.uppercase().filter { c -> !c.isWhitespace() } },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = {
                                Text("Search for Crib Number", fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal, fontFamily = FontFamily.Default,
                                    maxLines = 1, color = FeedCribColors.MutedText)
                            },
                            textStyle = TextStyle(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                color = FeedCribColors.DarkText
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(onGo = { if (number.isNotBlank()) onOpenCrib(number) }),
                            shape = RoundedCornerShape(11.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FeedCribColors.RustDeep,
                                unfocusedBorderColor = FeedCribColors.Line,
                                cursorColor = FeedCribColors.RustDeep
                            )
                        )
                        Spacer(Modifier.width(10.dp))
                        FeedCribTooltip("Open the existing crib matching the number you entered.") {
                            Button(
                                onClick = { onOpenCrib(number) },
                                enabled = number.isNotBlank(),
                                modifier = Modifier.height(56.dp),
                                shape = RoundedCornerShape(11.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FeedCribColors.Rust)
                            ) {
                                Text("Go", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                    if (errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(errorMessage, color = FeedCribColors.Refused, fontSize = 12.sp)
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeedCribTooltip("Review the cribs and readings captured today.", modifier = Modifier.weight(1f).height(50.dp)) {
                        Button(
                            onClick = onSessions,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FeedCribColors.RustDeep)
                        ) {
                            Text("Today's session", fontWeight = FontWeight.Bold)
                        }
                    }
                    FeedCribTooltip("Upload pending cribs and readings, then refresh reference information.", modifier = Modifier.weight(1f).height(50.dp)) {
                        OutlinedButton(
                            onClick = onSync,
                            enabled = !syncing,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (syncing) "Syncing…" else "Sync", fontWeight = FontWeight.Bold, color = FeedCribColors.RustDeep)
                        }
                    }
                }
            }

            if (syncMessage != null) {
                item {
                    Text(syncMessage, color = FeedCribColors.MutedText, fontSize = 12.sp)
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = lastDownloadedAt?.let { "Cribs downloaded ${formatDownloadTime(it)}" }
                            ?: "Cribs not downloaded yet",
                        modifier = Modifier.weight(1f),
                        color = FeedCribColors.MutedText,
                        fontSize = 11.sp
                    )
                    if (refreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = FeedCribColors.Rust
                        )
                    } else {
                        FeedCribTooltip("Download the latest cribs and reading codes. Saved local records are kept.") {
                            IconButton(onClick = onRefresh) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Download cribs", tint = FeedCribColors.RustDeep)
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search cribs…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FeedCribColors.Rust,
                        unfocusedBorderColor = FeedCribColors.Line,
                        focusedContainerColor = FeedCribColors.LightSurface,
                        unfocusedContainerColor = FeedCribColors.LightSurface
                    )
                )
            }

            if (visible.isEmpty()) {
                item {
                    Text(
                        text = if (cribs.isEmpty()) "No cribs on this phone yet. Connect and tap refresh to download them."
                        else "No crib matches that search.",
                        color = FeedCribColors.MutedText,
                        fontSize = 12.sp
                    )
                }
            }

            items(visible, key = { it.cribUuid }) { crib ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FeedCribColors.LightSurface, RoundedCornerShape(12.dp))
                        .clickable { onOpenCrib(crib.cribUuid) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        crib.cribNumber,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = FeedCribColors.DarkText,
                        modifier = Modifier.width(64.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        if (cribs.map { it.siteId }.distinct().size > 1) {
                            Text("Site: ${crib.siteId}", color = FeedCribColors.MutedText, fontSize = 11.sp)
                        }
                        if (crib.syncStatus != "SYNCED") {
                            Text(when (crib.syncStatus) {
                                "CONFLICT" -> "Creation conflict — resolution required"
                                "REJECTED" -> "Crib refused by the server — tap Sync to retry"
                                else -> "Crib awaiting sync"
                            }, color = FeedCribColors.Refused, fontSize = 11.sp)
                            crib.syncError?.let { Text(it, color = FeedCribColors.Refused, fontSize = 11.sp) }
                            if (crib.syncStatus == "CONFLICT" && onResolveConflict != null) {
                                FeedCribTooltip("Correct this crib’s conflicting code or tag so synchronisation can retry.") {
                                    TextButton(onClick = { onResolveConflict(crib.cribUuid) }) { Text("Resolve conflict") }
                                }
                            }
                        }
                        Text(
                            crib.penDescription.ifBlank { crib.description }.ifBlank { "—" },
                            fontSize = 13.sp,
                            color = FeedCribColors.DarkText
                        )
                        Text(
                            "${crib.ration.ifBlank { "No ration" }} · ADI ${formatKg(crib.currentAdi)}",
                            fontSize = 11.sp,
                            color = FeedCribColors.MutedText
                        )
                    }
                    Text("›", fontSize = 24.sp, color = FeedCribColors.RustDeep)
                }
            }
        }
    }
}

private fun formatDownloadTime(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))
