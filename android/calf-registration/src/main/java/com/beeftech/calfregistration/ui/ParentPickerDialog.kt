package com.beeftech.calfregistration.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeftech.calfregistration.data.ParentSearch

/** Lists this long or shorter are easy to scan; longer ones open with the keyboard ready to search. */
private const val AUTO_FOCUS_THRESHOLD = 8

/**
 * Searchable list for choosing a dam or sire. [noneOption] is the placeholder value that clears the choice.
 */
@Composable
fun ParentPickerDialog(
    title: String,
    options: List<String>,
    selected: String,
    noneOption: String,
    emptyMessage: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val matches = remember(query, options) { ParentSearch.filter(options, query) }
    val focusRequester = remember { FocusRequester() }

    if (options.size > AUTO_FOCUS_THRESHOLD) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold, color = BeeftechText) },
        text = {
            Column {
                if (options.isEmpty()) {
                    Text(
                        text = emptyMessage,
                        color = BeeftechText,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        singleLine = true,
                        placeholder = { Text("Search tag, e.g. B64 or 64, or breed", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (query.isBlank()) "${options.size} registered"
                        else "${matches.size} of ${options.size} match",
                        fontSize = 12.sp,
                        color = BeeftechMutedText
                    )

                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        item {
                            PickerRow(
                                text = "None",
                                isSelected = selected == noneOption,
                                onClick = { onSelect(noneOption) }
                            )
                        }
                        items(matches, key = { it }) { option ->
                            PickerRow(
                                text = option,
                                isSelected = option == selected,
                                onClick = { onSelect(option) }
                            )
                        }
                        if (matches.isEmpty()) {
                            item {
                                Text(
                                    text = "No animal matches '${query.trim()}'.",
                                    color = BeeftechMutedText,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = BeeftechPrimary) }
        },
        containerColor = BeeftechSurface
    )
}

@Composable
private fun PickerRow(text: String, isSelected: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
    ) {
        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) BeeftechPrimary else BeeftechText,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
