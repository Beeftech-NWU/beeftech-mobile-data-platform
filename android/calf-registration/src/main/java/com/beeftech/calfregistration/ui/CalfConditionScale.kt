package com.beeftech.calfregistration.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope

/**
 * Body condition as a row of five large equal-width boxes (1 Poor ... 5
 * Excellent), tall enough for gloved hands. Each box shows the score, its label
 * and a pip bar filled up to the score.
 */
@Composable
fun CalfConditionScale(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "CONDITION (1-5)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = BeeftechPrimaryDark
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CalfRegistrationLookups.conditionScores.forEach { score ->
                ConditionBox(
                    score = score,
                    isSelected = score == selected,
                    onClick = { onSelected(score) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.ConditionBox(
    score: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val filled = score.toInt()
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 72.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) BeeftechPrimary else BeeftechBorder
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) BeeftechPrimary else BeeftechWhite,
            contentColor = if (isSelected) BeeftechWhite else BeeftechText
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = score, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                text = CalfRegistrationLookups.conditionLabel(score),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (i in 1..5) {
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(4.dp)
                            .background(
                                color = if (i <= filled) {
                                    if (isSelected) BeeftechWhite else BeeftechPrimary
                                } else {
                                    if (isSelected) BeeftechWhite.copy(alpha = 0.35f) else BeeftechBorder
                                },
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}
