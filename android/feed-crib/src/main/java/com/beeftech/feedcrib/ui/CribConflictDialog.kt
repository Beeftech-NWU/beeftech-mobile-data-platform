package com.beeftech.feedcrib.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.beeftech.database.entity.FeedCribEntity

/** Change only identifiers rejected during creation; retain the UUID and all readings. */
@Composable
fun CribConflictDialog(
    crib: FeedCribEntity, saving: Boolean, errors: Map<String, String>,
    onDismiss: () -> Unit, onSave: (String, String) -> Unit
) {
    var code by rememberSaveable(crib.cribUuid) { mutableStateOf(crib.cribNumber) }
    var tag by rememberSaveable(crib.cribUuid) { mutableStateOf(crib.rfid.orEmpty()) }
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Resolve crib conflict") },
        text = { Column {
            Text("Choose a different code or tag. The existing UUID and readings are kept; cribs are never merged.")
            OutlinedTextField(value = code, onValueChange = { code = it }, enabled = !saving,
                label = { Text("Crib code") }, isError = errors["code"] != null,
                supportingText = { errors["code"]?.let { Text(it) } })
            OutlinedTextField(value = tag, onValueChange = { tag = it }, enabled = !saving,
                label = { Text("RFID tag (optional)") }, isError = errors["rfid"] != null,
                supportingText = { errors["rfid"]?.let { Text(it) } })
            errors["form"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            errors.filterKeys { it !in setOf("code", "rfid", "form") }.forEach { (field, message) ->
                Text("$field: $message", color = MaterialTheme.colorScheme.error)
            }
        } },
        confirmButton = { FeedCribTooltip("Save the corrected identifiers and retry synchronising this crib.") {
            TextButton(enabled = !saving, onClick = { onSave(code, tag) }) { Text("Save and retry") }
        } },
        dismissButton = { FeedCribTooltip("Close conflict resolution without saving changes.") {
            TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") }
        } }
    )
}

