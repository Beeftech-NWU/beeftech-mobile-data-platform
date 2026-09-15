package com.beeftech.farmtraceability.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TraceabilityHeader(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BeeftechPrimaryDeep)
            .padding(
                start = 14.dp,
                end = 22.dp,
                top = 22.dp,
                bottom = 20.dp
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = BeeftechWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))
            }

            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = BeeftechPrimary.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(11.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = BeeftechPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = eyebrow.uppercase(),
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BeeftechPrimary
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = title,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = BeeftechWhite
                )
            }
        }

        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(9.dp))

            Text(
                text = subtitle,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = BeeftechSoftAccent
            )
        }

        Spacer(modifier = Modifier.height(17.dp))

        HorizontalDivider(
            thickness = 2.dp,
            color = BeeftechPrimary
        )
    }
}

@Composable
fun TraceabilitySectionTitle(
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = 4.dp,
                    height = 18.dp
                )
                .background(
                    BeeftechPrimaryDark,
                    RoundedCornerShape(3.dp)
                )
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
fun TraceabilityTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = BeeftechPrimaryDark
        )

        Spacer(modifier = Modifier.height(7.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            minLines = minLines,
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            BeeftechSoftAccent,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = BeeftechPrimaryDark,
                        modifier = Modifier.size(19.dp)
                    )
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
    }
}

@Composable
fun TraceabilityDropdown(
    label: String,
    value: String,
    options: List<String>,
    icon: ImageVector,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = BeeftechPrimaryDark
        )

        Spacer(modifier = Modifier.height(7.dp))

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                placeholder = {
                    Text(
                        text = "Select $label",
                        color = BeeftechMutedText
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                BeeftechSoftAccent,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = BeeftechPrimaryDark,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = "Open options",
                        tint = BeeftechPrimaryDark
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                singleLine = true,
                shape = RoundedCornerShape(11.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BeeftechPrimaryDark,
                    unfocusedBorderColor = BeeftechBorder,
                    focusedContainerColor = BeeftechWhite,
                    unfocusedContainerColor = BeeftechWhite
                )
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expanded = true }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.88f)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = BeeftechText
                            )
                        },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TraceabilitySearchableDropdown(
    label: String,
    value: String,
    options: List<String>,
    icon: ImageVector,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    var searchText by remember(value) {
        mutableStateOf(value)
    }

    val filteredOptions = remember(searchText, options) {
        if (searchText.isBlank()) {
            options
        } else {
            options.filter { option ->
                option.contains(
                    other = searchText,
                    ignoreCase = true
                )
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = BeeftechPrimaryDark
        )

        Spacer(modifier = Modifier.height(7.dp))

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { input ->
                    searchText = input
                    expanded = true

                    if (input != value) {
                        onValueChange("")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        text = "Select $label",
                        color = BeeftechMutedText
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                BeeftechSoftAccent,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = BeeftechPrimaryDark,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            expanded = !expanded
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowDropDown,
                            contentDescription = "Open options",
                            tint = BeeftechPrimaryDark
                        )
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

            DropdownMenu(
                expanded = expanded && filteredOptions.isNotEmpty(),
                onDismissRequest = {
                    expanded = false
                },
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .heightIn(max = 280.dp)
            ) {
                filteredOptions.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = BeeftechText
                            )
                        },
                        onClick = {
                            searchText = option
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Suppress("unused")
@Composable
fun TraceabilityChoiceSelector(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = BeeftechPrimaryDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val selected = option == selectedValue

                OutlinedButton(
                    onClick = {
                        onValueChange(option)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) {
                            BeeftechSoftAccent
                        } else {
                            BeeftechWhite
                        },
                        contentColor = BeeftechPrimaryDeep
                    )
                ) {
                    Text(
                        text = option,
                        fontSize = 12.sp,
                        fontWeight = if (selected) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Medium
                        }
                    )
                }
            }
        }
    }
}

@Suppress("unused")
@Composable
fun TraceabilityStatusBadge(
    status: String
) {
    val backgroundColor: Color
    val textColor: Color

    when (status.lowercase()) {
        "registered" -> {
            backgroundColor = BeeftechSoftAccent
            textColor = BeeftechPrimaryDeep
        }

        "processing" -> {
            backgroundColor = BeeftechPrimary.copy(alpha = 0.18f)
            textColor = BeeftechPrimaryDeep
        }

        else -> {
            backgroundColor = BeeftechSurface
            textColor = BeeftechPrimaryDark
        }
    }

    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(50.dp)
            )
            .padding(
                horizontal = 12.dp,
                vertical = 7.dp
            )
    ) {
        Text(
            text = status,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Suppress("unused")
@Composable
fun TraceabilityReadOnlyField(
    label: String,
    value: String,
    icon: ImageVector,
    placeholder: String = "Generated automatically"
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = BeeftechPrimaryDark
        )

        Spacer(modifier = Modifier.height(7.dp))

        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            placeholder = {
                Text(
                    text = placeholder,
                    color = BeeftechMutedText
                )
            },
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            BeeftechSoftAccent,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = BeeftechPrimaryDark,
                        modifier = Modifier.size(19.dp)
                    )
                }
            },
            shape = RoundedCornerShape(11.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BeeftechBorder,
                unfocusedBorderColor = BeeftechBorder,
                disabledBorderColor = BeeftechBorder,
                focusedContainerColor = BeeftechSurface,
                unfocusedContainerColor = BeeftechSurface
            )
        )
    }
}

@Composable
fun TraceabilityCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = BeeftechSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(17.dp)
        ) {
            content()
        }
    }
}

@Composable
fun TraceabilityPrimaryButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BeeftechPrimaryDeep,
            contentColor = BeeftechWhite
        ),
        shape = RoundedCornerShape(11.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(19.dp)
        )

        Spacer(modifier = Modifier.width(9.dp))

        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun TraceabilitySecondaryButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(11.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = BeeftechPrimaryDeep
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(19.dp)
        )

        Spacer(modifier = Modifier.width(9.dp))

        Text(
            text = text,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun TraceabilityInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    value: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    BeeftechSoftAccent,
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BeeftechPrimaryDark,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = BeeftechText
            )

            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = BeeftechMutedText
                )
            }
        }

        if (value.isNotBlank()) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = BeeftechPrimaryDark
            )
        }
    }
}

@Composable
fun TraceabilitySyncWarning(
    warningLevel: Int,
    onSyncClick: () -> Unit = {}
) {
    val warningTitle: String
    val warningMessage: String

    when (warningLevel) {
        1 -> {
            warningTitle = "Data Not Synced"
            warningMessage =
                "Data has not been synced. Please connect to a network and sync your records."
        }

        2 -> {
            warningTitle = "Critical Sync Warning"
            warningMessage =
                "Data is still not synced. Please connect and sync your records as soon as possible."
        }

        3 -> {
            warningTitle = "Final Sync Warning"
            warningMessage =
                "Your records have not been synced. Please connect and sync your data today."
        }

        else -> return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = BeeftechSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(17.dp)
        ) {
            Text(
                text = warningTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BeeftechPrimaryDeep
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = warningMessage,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = BeeftechMutedText
            )

            Spacer(modifier = Modifier.height(15.dp))

            TraceabilitySecondaryButton(
                text = "Sync Now",
                icon = Icons.Outlined.Refresh,
                onClick = onSyncClick
            )
        }
    }
}