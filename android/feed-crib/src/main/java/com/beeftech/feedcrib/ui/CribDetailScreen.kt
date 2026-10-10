package com.beeftech.feedcrib.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeftech.database.entity.CribReadingCodeEntity
import com.beeftech.feedcrib.viewmodel.CribDetailState

/** One crib: its details, the last nine readings, the code and ADI to enter, and Discard / Save. */
@Composable
fun CribDetailScreen(
    detail: CribDetailState,
    codes: List<CribReadingCodeEntity>,
    nowMillis: Long,
    saving: Boolean,
    onSelectCode: (Int) -> Unit,
    onAdjustAdi: (Int) -> Unit,
    onDiscard: () -> Unit,
    onSave: () -> Unit
) {
    val crib = detail.crib
    var confirmDiscard by remember { mutableStateOf(false) }

    fun requestDiscard() {
        if (detail.draft.changed) confirmDiscard = true else onDiscard()
    }

    BackHandler { requestDiscard() }

    Column(modifier = Modifier.fillMaxSize().background(FeedCribColors.LightBg)) {
        FeedHeader(
            eyebrow = "Crib reading",
            title = crib.cribNumber,
            subtitle = crib.penDescription.ifBlank { crib.description },
            icon = Icons.Outlined.Restaurant,
            onBackClick = ::requestDiscard
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FeedCard {
                FeedDetailRow("Pen", crib.penDescription)
                FeedDetailRow("Ration", crib.ration)
                FeedDetailRow("Method", crib.method)
                FeedDetailRow("Description", crib.description)
                FeedDetailRow("Required", formatKg(crib.requiredKg))
                FeedDetailRow("A.D.I", formatKg(crib.currentAdi))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FeedCribColors.InkLight, RoundedCornerShape(10.dp))
            ) {
                listOf(
                    "Begin" to crib.animalsBegin,
                    "In" to crib.animalsIn,
                    "Out" to crib.animalsOut,
                    "Close" to crib.animalsClose
                ).forEach { (label, value) ->
                    Column(
                        modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(label.uppercase(), color = FeedCribColors.MutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text(value.toString(), color = FeedCribColors.RustDeep, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            FeedSectionTitle("Last readings")
            ReadingsGrid(detail = detail, nowMillis = nowMillis)

            FeedSectionTitle("Reading code")
            CodeChips(codes = codes, selected = detail.draft.code, onSelect = onSelectCode)
            Text(
                text = if (detail.draft.code != null) goesToText(detail.currentSlot)
                else "Pick a code. It goes to the block for the time of day.",
                color = FeedCribColors.RustDeep,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            FeedSectionTitle("A.D.I")
            AdiStepper(adi = detail.draft.adi, onAdjust = onAdjustAdi)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FeedCribColors.LightSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = ::requestDiscard,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, FeedCribColors.Line)
            ) {
                Text("Discard", color = FeedCribColors.MutedText, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onSave,
                enabled = detail.draft.changed && !saving,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FeedCribColors.Rust)
            ) {
                Text(if (saving) "Saving…" else "Save", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard this reading?") },
            text = { Text("What you picked for ${crib.cribNumber} will not be saved.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onDiscard() }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") }
            }
        )
    }
}

/** Rows are the last 3 days (today first), columns are M / D / E. The block a reading would go into now is outlined. */
@Composable
private fun ReadingsGrid(detail: CribDetailState, nowMillis: Long) {
    val dates = gridDates(detail.currentDate, nowMillis)

    FeedCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(64.dp))
            GRID_SLOTS.forEach { slot ->
                Text(
                    slotInitial(slot),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = FeedCribColors.MutedText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
        dates.forEachIndexed { index, date ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (index == 0) "Today" else shortDate(date),
                    modifier = Modifier.width(64.dp),
                    color = FeedCribColors.MutedText,
                    fontSize = 11.sp
                )
                GRID_SLOTS.forEach { slot ->
                    val current = date == detail.currentDate && slot == detail.currentSlot
                    val code = gridCode(detail.lastSlots, date, slot)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 3.dp)
                            .height(40.dp)
                            .background(if (current) FeedCribColors.HeaderSoft else FeedCribColors.InkLight, RoundedCornerShape(8.dp))
                            .border(
                                width = if (current) 2.dp else 0.dp,
                                color = if (current) FeedCribColors.Rust else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            code?.toString() ?: "–",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (code != null) FeedCribColors.DarkText else FeedCribColors.MutedText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeChips(codes: List<CribReadingCodeEntity>, selected: Int?, onSelect: (Int) -> Unit) {
    if (codes.isEmpty()) {
        Text("No reading codes yet. Download the cribs to get them.", color = FeedCribColors.MutedText, fontSize = 12.sp)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        codes.filter { it.active }.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { code ->
                    val isSelected = code.code == selected
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .background(
                                if (isSelected) FeedCribColors.Rust else FeedCribColors.LightSurface,
                                RoundedCornerShape(11.dp)
                            )
                            .border(1.dp, if (isSelected) FeedCribColors.Rust else FeedCribColors.Line, RoundedCornerShape(11.dp))
                            .clickable { onSelect(code.code) }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            code.code.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = if (isSelected) Color.White else FeedCribColors.RustDeep
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            code.label,
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            color = if (isSelected) Color.White else FeedCribColors.DarkText
                        )
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AdiStepper(adi: Double, onAdjust: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StepButton(onClick = { onAdjust(-1) }) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease ADI", tint = FeedCribColors.RustDeep)
        }
        Text(
            formatKg(adi),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = FeedCribColors.DarkText
        )
        StepButton(onClick = { onAdjust(1) }) {
            Icon(Icons.Default.Add, contentDescription = "Increase ADI", tint = FeedCribColors.RustDeep)
        }
    }
}

@Composable
private fun StepButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(52.dp)
            .background(FeedCribColors.LightSurface, RoundedCornerShape(11.dp))
            .border(1.5.dp, FeedCribColors.Line, RoundedCornerShape(11.dp))
    ) { content() }
}
