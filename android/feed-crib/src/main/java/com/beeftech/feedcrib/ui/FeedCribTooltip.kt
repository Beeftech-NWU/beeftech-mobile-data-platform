package com.beeftech.feedcrib.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Material tooltips support mouse hover and touch long-press without replacing button clicks. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedCribTooltip(
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Keep Row weights and Box alignment on the outer layout node, rather than the anchor.
    Box(modifier = modifier, propagateMinConstraints = true) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(description) } },
            state = rememberTooltipState(),
            content = content
        )
    }
}
