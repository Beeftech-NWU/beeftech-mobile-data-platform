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
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
    icon: ImageVector = Icons.Outlined.EditNote,
    onFocusLost: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    // onFocusChanged also reports "unfocused" on first composition, so only react after real focus.
    var hadFocus by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = BeeftechPrimaryDark)
        Spacer(modifier = Modifier.height(7.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = BeeftechMutedText, fontSize = 14.sp) },
            singleLine = true,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            leadingIcon = {
                Box(
                    modifier = Modifier.size(34.dp).background(BeeftechSoftAccent, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = BeeftechPrimaryDark, modifier = Modifier.size(19.dp))
                }
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
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(47.dp).background(BeeftechSoftAccent, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = BeeftechPrimaryDark, modifier = Modifier.size(23.dp))
            }
            Spacer(modifier = Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BeeftechText)
                Spacer(modifier = Modifier.height(3.dp))
                Text(subtitle, fontSize = 11.sp, lineHeight = 15.sp, color = BeeftechMutedText)
            }
            Text("›", fontSize = 26.sp, fontWeight = FontWeight.Medium, color = BeeftechPrimaryDark)
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
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (i in 1..totalSteps) {
            val isActive = i <= currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(
                        color = if (isActive) BeeftechPrimary else BeeftechBorder,
                        shape = RoundedCornerShape(3.dp)
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BeeftechWhite, RoundedCornerShape(14.dp))
            .border(1.dp, BeeftechBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = BeeftechMutedText
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value.ifEmpty { "Not set" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BeeftechText
            )
        }

        TextButton(
            onClick = onChangeClick,
            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        ) {
            Text(
                text = "Change",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BeeftechPrimary
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
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

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(
                    color = bg,
                    shape = RoundedCornerShape(
                        topStart = 10.dp,
                        topEnd = 10.dp,
                        bottomStart = 24.dp,
                        bottomEnd = 24.dp
                    )
                )
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 3.dp,
                            color = BeeftechText,
                            shape = RoundedCornerShape(
                                topStart = 10.dp,
                                topEnd = 10.dp,
                                bottomStart = 24.dp,
                                bottomEnd = 24.dp
                            )
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Text(
                    text = "✓",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = colour.name.lowercase().replaceFirstChar { it.uppercase() },
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) BeeftechText else BeeftechMutedText
        )
    }
}
