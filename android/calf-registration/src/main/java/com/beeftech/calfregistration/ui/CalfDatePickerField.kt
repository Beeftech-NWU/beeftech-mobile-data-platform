package com.beeftech.calfregistration.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalfDatePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    supportingText: String? = null,
    maxToday: Boolean = true,
    infoText: String? = null
) {
    val context = LocalContext.current

    fun openPicker() {
        val initial =
            runCatching {
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                ).apply {
                    isLenient = false
                }.parse(value)
            }.getOrNull()
                ?: Date()

        val calendar =
            Calendar.getInstance().apply {
                time = initial
            }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onValueChange(
                    String.format(
                        Locale.getDefault(),
                        "%02d/%02d/%04d",
                        dayOfMonth,
                        month + 1,
                        year
                    )
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            if (maxToday) {
                datePicker.maxDate =
                    System.currentTimeMillis()
            }
        }.show()
    }

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        if (infoText.isNullOrBlank()) {
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = BeeftechPrimaryDark
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = BeeftechPrimaryDark
                )
                Spacer(modifier = Modifier.width(4.dp))
                CalfBirthHelpIcon(title = label, message = infoText)
            }
        }

        Spacer(
            modifier = Modifier.height(7.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            placeholder = {
                Text(
                    text = "DD/MM/YYYY",
                    color = BeeftechMutedText,
                    fontSize = 14.sp
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
                    contentAlignment =
                        Alignment.Center
                ) {
                    Icon(
                        imageVector =
                            Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = BeeftechPrimaryDark,
                        modifier =
                            Modifier.size(19.dp)
                    )
                }
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        openPicker()
                    }
                ) {
                    Icon(
                        imageVector =
                            Icons.Outlined.CalendarMonth,
                        contentDescription =
                            "Choose $label",
                        tint = BeeftechPrimaryDark
                    )
                }
            },
            modifier =
                Modifier.fillMaxWidth(),
            singleLine = true,
            shape =
                RoundedCornerShape(11.dp),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor =
                        BeeftechPrimaryDark,
                    unfocusedBorderColor =
                        BeeftechBorder,
                    focusedContainerColor =
                        BeeftechWhite,
                    unfocusedContainerColor =
                        BeeftechWhite
                )
        )

        if (!supportingText.isNullOrBlank()) {
            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text = supportingText,
                fontSize = 11.sp,
                color = BeeftechMutedText
            )
        }
    }
}

/** Contextual help used only by the Birth Details section of Calf Registration. */
@Composable
internal fun CalfBirthHelpIcon(title: String, message: String) {
    var showHelp by remember { mutableStateOf(false) }
    IconButton(
        onClick = { showHelp = true },
        modifier = Modifier.size(30.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = "Information about $title",
            tint = BeeftechPrimaryDark,
            modifier = Modifier.size(18.dp)
        )
    }
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text("OK") }
            }
        )
    }
}
