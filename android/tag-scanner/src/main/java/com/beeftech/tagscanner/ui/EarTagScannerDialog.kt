package com.beeftech.tagscanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun EarTagScannerDialog(
    onDismiss: () -> Unit,
    onTagScanned: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        EarTagScannerScreen(
            onTagScanned = { id ->
                onTagScanned(id)
                onDismiss()
            },
            onCancel = onDismiss
        )
    }
}
