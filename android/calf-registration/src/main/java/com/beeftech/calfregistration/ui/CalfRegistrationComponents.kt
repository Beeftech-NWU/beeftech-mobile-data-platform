package com.beeftech.calfregistration.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
fun CalfHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBackClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BeeftechPrimaryDeep)
            .padding(start = 14.dp, end = 22.dp, top = 22.dp, bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showBackButton) {
                IconButton(onClick = { onBackClick?.invoke() }, modifier = Modifier.size(42.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = BeeftechWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(BeeftechPrimary.copy(alpha = 0.18f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = BeeftechPrimary, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eyebrow.uppercase(),
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BeeftechPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = title, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = BeeftechWhite)
            }
        }

        Spacer(modifier = Modifier.height(9.dp))
        Text(text = subtitle, fontSize = 12.sp, lineHeight = 17.sp, color = BeeftechSoftAccent)
        Spacer(modifier = Modifier.height(17.dp))
        HorizontalDivider(thickness = 2.dp, color = BeeftechPrimary)
    }
}

@Composable
fun CalfSectionTitle(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 18.dp)
                .background(BeeftechPrimaryDark, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = BeeftechPrimaryDark
        )
    }
}

@Composable
fun CalfCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BeeftechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(17.dp), content = content)
    }
}

@Composable
fun LookupTagBadge(label: String = "LOOKUP", modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(BeeftechSoftAccent, RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = BeeftechPrimaryDark
        )
    }
}

@Composable
fun CalfTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "—",
    supportingText: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.EditNote
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = BeeftechPrimaryDark)
        Spacer(modifier = Modifier.height(7.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = BeeftechMutedText, fontSize = 14.sp) },
            singleLine = true,
            leadingIcon = {
                Box(
                    modifier = Modifier.size(34.dp).background(BeeftechSoftAccent, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = BeeftechPrimaryDark, modifier = Modifier.size(19.dp))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(11.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BeeftechPrimaryDark,
                unfocusedBorderColor = BeeftechBorder,
                cursorColor = BeeftechPrimaryDark,
                focusedContainerColor = BeeftechWhite,
                unfocusedContainerColor = BeeftechWhite
            )
        )
        if (!supportingText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = supportingText, fontSize = 11.sp, color = BeeftechMutedText)
        }
    }
}

@Composable
fun CalfLookupDropdownField(
    label: String,
    badgeLabel: String = "LOOKUP",
    selectedValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = BeeftechPrimaryDark)
            Spacer(modifier = Modifier.width(6.dp))
            LookupTagBadge(label = badgeLabel)
        }
        Spacer(modifier = Modifier.height(7.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(BeeftechWhite, RoundedCornerShape(11.dp))
                .border(1.dp, BeeftechBorder, RoundedCornerShape(11.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(34.dp).background(BeeftechSoftAccent, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ListAlt, contentDescription = null, tint = BeeftechPrimaryDark, modifier = Modifier.size(19.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = selectedValue.ifEmpty { "Select an option" },
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (selectedValue.startsWith("Select")) BeeftechMutedText else BeeftechText
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select", tint = BeeftechPrimaryDark)
        }
    }
}

@Composable
fun CalfPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BeeftechPrimaryDeep, contentColor = BeeftechWhite),
        shape = RoundedCornerShape(11.dp)
    ) {
        Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(19.dp))
        Spacer(modifier = Modifier.width(9.dp))
        Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun CalfSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(11.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = BeeftechPrimaryDeep)
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}
