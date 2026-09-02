package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CalfRegistration
import com.example.ui.theme.*

/**
 * Realistic Livestock Ear Tag visual component.
 * Modeled after standard Allflex / Leader livestock tags with punch-hole,
 * bold stamped ID, RFID icon, and breed/sex footer.
 */
@Composable
fun LivestockEarTagVisual(
    tagNumber: String,
    breed: String,
    sex: String,
    rfidTag: String? = null,
    tagColor: Color = EarTagYellow,
    tagDarkColor: Color = EarTagYellowDark,
    isLarge: Boolean = false,
    modifier: Modifier = Modifier
) {
    val height = if (isLarge) 110.dp else 74.dp
    val width = if (isLarge) 130.dp else 92.dp

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .shadow(if (isLarge) 4.dp else 2.dp, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
            .background(tagColor)
            .border(
                width = 1.5.dp,
                color = tagDarkColor.copy(alpha = 0.6f),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
            )
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("livestock_ear_tag_${tagNumber.ifEmpty { "empty" }}"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Ear tag fastener hole + RFID bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stud hole
                Box(
                    modifier = Modifier
                        .size(if (isLarge) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF78350F).copy(alpha = 0.7f))
                )

                Text(
                    text = "BEEFTECH",
                    fontSize = if (isLarge) 8.sp else 6.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF78350F)
                )

                // RFID chip symbol
                Icon(
                    imageVector = Icons.Default.Nfc,
                    contentDescription = "RFID",
                    tint = Color(0xFF78350F),
                    modifier = Modifier.size(if (isLarge) 12.dp else 9.dp)
                )
            }

            // Big Stamped Animal ID
            Text(
                text = if (tagNumber.isBlank()) "ZA-0000" else tagNumber,
                color = Color(0xFF1E293B),
                fontSize = if (isLarge) 17.sp else 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-0.5).sp,
                maxLines = 1
            )

            // Bottom metadata strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.1f))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = breed.take(8),
                    fontSize = if (isLarge) 9.sp else 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = when (sex) {
                        CalfRegistration.SEX_HEIFER -> "♀ HFR"
                        CalfRegistration.SEX_BULL -> "♂ BUL"
                        else -> "STR"
                    },
                    fontSize = if (isLarge) 9.sp else 7.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1E293B)
                )
            }
        }
    }
}

@Composable
fun SexPill(
    sex: String,
    modifier: Modifier = Modifier
) {
    val isHeifer = sex.equals(CalfRegistration.SEX_HEIFER, ignoreCase = true)
    val isBull = sex.equals(CalfRegistration.SEX_BULL, ignoreCase = true)

    val bgColor = when {
        isHeifer -> SexHeiferBg
        isBull -> SexBullBg
        else -> SexSteerBg
    }
    val textColor = when {
        isHeifer -> SexHeiferText
        isBull -> SexBullText
        else -> SexSteerText
    }
    val icon = when {
        isHeifer -> Icons.Default.Female
        isBull -> Icons.Default.Male
        else -> null
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = sex,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
        }
        Text(
            text = sex,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CalvingEaseChip(
    score: Int,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (score) {
        CalfRegistration.CALVING_EASE_UNASSISTED -> "1: Normal" to Color(0xFF047857)
        CalfRegistration.CALVING_EASE_EASY_PULL -> "2: Easy Pull" to Color(0xFFB45309)
        CalfRegistration.CALVING_EASE_HARD_PULL -> "3: Hard Pull" to Color(0xFFC2410C)
        else -> "4: Caesarean" to Color(0xFFB91C1C)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
