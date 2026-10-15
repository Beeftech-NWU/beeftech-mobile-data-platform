package com.beeftech.calfregistration.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

/** Single-row screen header (about 56dp): back, icon chip, title and an optional one-line subtitle. */
@Composable
fun CalfHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showBackButton: Boolean = false,
    onBackClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BeeftechHeaderBackground)
            .defaultMinSize(minHeight = 56.dp)
            .padding(start = if (showBackButton) 4.dp else 14.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackButton) {
            IconButton(onClick = { onBackClick?.invoke() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = BeeftechWhite
                )
            }
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .background(BeeftechHeaderAccent.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = BeeftechHeaderAccent, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BeeftechWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = BeeftechHeaderSubtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
        Column(modifier = Modifier.padding(12.dp), content = content)
    }
}

/** Lays out fields side by side with equal widths. */
@Composable
fun CalfFieldRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
        content = content
    )
}

/** Small uppercase field label, with an optional help icon. */
@Composable
fun CalfFieldLabel(label: String, infoText: String? = null) {
    Row(
        modifier = Modifier.defaultMinSize(minHeight = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = BeeftechPrimaryDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (!infoText.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(2.dp))
            CalfBirthHelpIcon(title = label, message = infoText)
        }
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
    icon: ImageVector? = null,
    onFocusLost: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    infoText: String? = null
) {
    // onFocusChanged also reports "unfocused" on first composition, so only react after real focus.
    var hadFocus by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        CalfFieldLabel(label = label, infoText = infoText)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(placeholder, color = BeeftechMutedText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            singleLine = true,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            leadingIcon = icon?.let {
                { Icon(it, contentDescription = null, tint = BeeftechPrimaryDark, modifier = Modifier.size(20.dp)) }
            },
            modifier = Modifier.fillMaxWidth().onFocusChanged { state ->
                if (state.isFocused) {
                    hadFocus = true
                } else if (hadFocus) {
                    hadFocus = false
                    onFocusLost?.invoke()
                }
            },
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
    badgeLabel: String? = null,
    selectedValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (badgeLabel == null) {
            CalfFieldLabel(label = label)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CalfFieldLabel(label = label)
                Spacer(modifier = Modifier.width(6.dp))
                LookupTagBadge(label = badgeLabel)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(BeeftechWhite, RoundedCornerShape(11.dp))
                .border(1.dp, BeeftechBorder, RoundedCornerShape(11.dp))
                .clickable(onClick = onClick)
                .padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedValue.ifEmpty { "Select an option" },
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (selectedValue.startsWith("Select")) BeeftechMutedText else BeeftechText
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select", tint = BeeftechPrimaryDark)
        }
    }
}

@Composable
fun CalfPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
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
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(11.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = BeeftechPrimaryDeep)
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun CalfMenuCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BeeftechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(BeeftechSoftAccent, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = BeeftechPrimaryDark, modifier = Modifier.size(21.dp))
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BeeftechText)
                Spacer(modifier = Modifier.height(3.dp))
                Text(subtitle, fontSize = 11.sp, lineHeight = 15.sp, color = BeeftechMutedText)
            }
            Text("›", fontSize = 26.sp, fontWeight = FontWeight.Medium, color = BeeftechPrimaryDark)
        }
    }
}

/** One compact header for every wizard step: back arrow, "Step N of 4 · title", and the progress bar. */
@Composable
fun CalfStepTopBar(
    step: Int,
    title: String,
    onBackClick: (() -> Unit)?,
    totalSteps: Int = 4
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BeeftechSurface,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(start = 4.dp, end = 16.dp, bottom = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = BeeftechText
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = "Step $step of $totalSteps",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BeeftechPrimaryDark
                )
                Text(
                    text = "  ·  $title",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BeeftechText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StepProgress(
                currentStep = step,
                totalSteps = totalSteps,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
fun StepProgress(
    currentStep: Int,
    totalSteps: Int = 4,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 1..totalSteps) {
            val isActive = i <= currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .background(
                        color = if (isActive) BeeftechPrimary else BeeftechBorder,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
fun ReviewRow(
    label: String,
    value: String,
    onChangeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Rows sit together inside one CalfCard on the review screen, so no border or background here.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = BeeftechMutedText
            )
            Text(
                text = value.ifEmpty { "Not set" },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BeeftechText
            )
        }

        IconButton(onClick = onChangeClick) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Change $label",
                tint = BeeftechPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun BottomActionDock(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = BeeftechSurface,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, BeeftechBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
fun InlineValidationMessage(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isError) Color(0xFF8C1D18) else Color(0xFF17402D)
    val background = if (isError) Color(0xFFF9DAD7) else Color(0xFFDDEFE4)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = background,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isError) "⚠" else "✓",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = message,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
fun TagColorCard(
    colour: com.beeftech.database.util.TagColour,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when (colour) {
        com.beeftech.database.util.TagColour.BLUE -> Color(0xFF1D5FB8)
        com.beeftech.database.util.TagColour.RED -> Color(0xFFC62828)
        com.beeftech.database.util.TagColour.GREEN -> Color(0xFF2E7D32)
        com.beeftech.database.util.TagColour.YELLOW -> Color(0xFFE0A800)
    }

    val shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 22.dp, bottomEnd = 22.dp)
    val name = colour.name.lowercase().replaceFirstChar { it.uppercase() }

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(color = bg, shape = shape)
            .then(if (isSelected) Modifier.border(width = 3.dp, color = BeeftechText, shape = shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isSelected) "✓ $name" else name,
            color = if (colour == com.beeftech.database.util.TagColour.YELLOW) BeeftechText else Color.White,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}
