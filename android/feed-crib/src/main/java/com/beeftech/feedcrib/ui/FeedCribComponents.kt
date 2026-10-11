package com.beeftech.feedcrib.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Slides a header off screen as the list below scrolls down and back in as it scrolls up.
 * Pair [collapsingHeader] on the header with [nestedScrollConnection] on the scrollable.
 */
@Stable
class CollapsingHeaderState {
    internal var heightPx by mutableFloatStateOf(0f)

    /** 0 when fully shown, down to -[heightPx] when fully hidden. */
    internal var offsetPx by mutableFloatStateOf(0f)

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            offsetPx = (offsetPx + available.y).coerceIn(-heightPx, 0f)
            return Offset.Zero
        }
    }
}

@Composable
fun rememberCollapsingHeaderState(): CollapsingHeaderState = remember { CollapsingHeaderState() }

/** Shrinks the header's layout height by the amount it has been scrolled away, so the list below reclaims the space. */
fun Modifier.collapsingHeader(state: CollapsingHeaderState): Modifier =
    clipToBounds().layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        if (state.heightPx != placeable.height.toFloat()) state.heightPx = placeable.height.toFloat()
        val offset = state.offsetPx.coerceIn(-placeable.height.toFloat(), 0f).roundToInt()
        layout(placeable.width, placeable.height + offset) {
            placeable.placeRelative(0, offset)
        }
    }

/** The same header the calf screens use, in the feed crib's sage colours. */
@Composable
fun FeedHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FeedCribColors.HeaderBg)
            .padding(start = 14.dp, end = 22.dp, top = 22.dp, bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBackClick != null) {
                FeedCribTooltip("Return to the previous Feed Cribs screen.") {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(42.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(FeedCribColors.HeaderSoft.copy(alpha = 0.22f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = FeedCribColors.HeaderSoft, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eyebrow.uppercase(),
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FeedCribColors.HeaderAccent
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = title, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(9.dp))
        Text(text = subtitle, fontSize = 12.sp, lineHeight = 17.sp, color = FeedCribColors.HeaderAccent)
        Spacer(modifier = Modifier.height(17.dp))
        HorizontalDivider(thickness = 2.dp, color = FeedCribColors.HeaderAccent)
    }
}

@Composable
fun FeedSectionTitle(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 18.dp)
                .background(FeedCribColors.RustDeep, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = FeedCribColors.RustDeep
        )
    }
}

@Composable
fun FeedCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FeedCribColors.LightSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun FeedDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = FeedCribColors.MutedText, fontSize = 12.sp)
        Text(
            value.ifBlank { "—" },
            color = FeedCribColors.DarkText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** A small rounded pill: "Waiting", "Synced", "Refused". */
@Composable
fun FeedPill(text: String, textColor: Color, background: Color) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(50.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

/** The pill for an entry's sync status ("PENDING", "SYNCED", "REJECTED"). */
@Composable
fun SyncStatusPill(syncStatus: String) {
    when (syncStatus) {
        "SYNCED" -> FeedPill("Synced", FeedCribColors.Synced, FeedCribColors.SyncedBg)
        "REJECTED" -> FeedPill("Refused", FeedCribColors.Refused, FeedCribColors.RefusedBg)
        else -> FeedPill("Waiting", FeedCribColors.Waiting, FeedCribColors.WaitingBg)
    }
}
